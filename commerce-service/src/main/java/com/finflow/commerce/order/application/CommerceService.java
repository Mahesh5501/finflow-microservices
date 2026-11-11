package com.finflow.commerce.order.application;

import com.finflow.commerce.order.dto.CreateOrderRequest;
import com.finflow.commerce.order.dto.OrderResponse;
import com.finflow.commerce.order.domain.CommerceOrder;
import com.finflow.commerce.order.domain.OrderStatus;
import com.finflow.commerce.integration.event.CommerceEventPublisher;
import com.finflow.commerce.integration.event.CommerceEventType;
import com.finflow.commerce.order.application.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.order.dto.OrderItemRequest;
import com.finflow.commerce.catalogue.domain.Product;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import com.finflow.commerce.order.persistence.CommerceOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

/**
 * Main commerce orchestration service.
 *
 * It coordinates order placement,
 * payment settlement, cancellation and fulfilment operations.
 *
 * It does not implement every business operation itself.
 * Instead, it delegates specialised responsibilities to other services.
 *
 * Order application facade used by customer/merchant order controllers and
 * CartService. It coordinates OrderPlacementService, PaymentSettlementService and
 * OrderCancelService, and uses order/product repositories for reads, seller validation
 * and fulfilment; CommerceEventPublisher isolates event transport. It owns no catalogue
 * or coupon management endpoints. Constructor injection exposes these dependencies.
 *
 * @Service provides the injectable facade; explicit @Bean registration is an alternative.
 * createOrder/payOrder/cancelOrder intentionally have no @Transactional annotation:
 * wallet HTTP must follow placement commit and precede local refund finalization.
 * Do not add a class-wide transaction. Read/fulfilment annotations open short REQUIRED
 * transactions so lazy items can be mapped and state changes commit together.
 * Removing them can expose detached collections or split the local unit of work.
 *
 * Flow: infer/validate a single seller -> commit pending order/stock/key/payment work
 * -> charge through PaymentGateway -> settle locally -> return OrderResponse.
 * Customer history uses user ID; merchant list/fulfilment uses JWT organisation IDs.
 * getOrder also permits the owner or seller staff, matching the existing contract.
 * Fulfilment events currently publish inside the transaction; reliable delivery remains
 * separate work, not a guarantee supplied by the payment outbox.
 */
@Service
public class CommerceService {

    /*
     * These are the order statuses from which cancellation is allowed.
     *
     * EnumSet is efficient for working with enum values.
     *
     * Example:
     * CANCELLABLE.contains(OrderStatus.CONFIRMED) -> true
     */
    private static final EnumSet<OrderStatus> CANCELLABLE = EnumSet.of(
            OrderStatus.CREATED,
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.ACTION_REQUIRED,
            OrderStatus.CONFIRMED
    );

    /*
     * Specialised services and repositories used by this orchestrator.
     */
    private final ProductRepository productRepository;
    private final CommerceOrderRepository orderRepository;
    private final OrderPlacementService orderPlacementService;
    private final PaymentSettlementService paymentSettlementService;
    private final OrderCancelService orderCancelService;
    private final CommerceEventPublisher eventPublisher;

    /*
     * Constructor injection.
     *
     * Spring provides these dependencies when creating CommerceService.
     *
     * final ensures that these references cannot be reassigned after
     * the object has been constructed.
     */
    public CommerceService(
            ProductRepository productRepository,
            CommerceOrderRepository orderRepository,
            OrderPlacementService orderPlacementService,
            PaymentSettlementService paymentSettlementService,
            OrderCancelService orderCancelService,
            CommerceEventPublisher eventPublisher
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderPlacementService = orderPlacementService;
        this.paymentSettlementService = paymentSettlementService;
        this.orderCancelService = orderCancelService;
        this.eventPublisher = eventPublisher;
    }

    /*
     * ORDER CREATION
     *
     * This method coordinates two separate stages:
     *
     * 1. Place the pending order and reserve stock.
     * 2. Settle the payment.
     *
     * The detailed business logic is delegated to specialised services.
     */
    public OrderResponse createOrder(
            Long customerUserId,
            List<Long> callerOrgIds,
            String idempotencyKey,
            String accessToken,
            CreateOrderRequest request
    ) {

        Long sellerOrganisationId = resolveSellerFromItems(
                request.organisationId(),
                request.items()
        );

        Long orderId = orderPlacementService.placePendingOrder(
                customerUserId,
                callerOrgIds,
                idempotencyKey,
                sellerOrganisationId,
                request.items(),
                request.shippingAddress(),
                request.couponCode(),
                request.paymentOutcome()
        );

        // After placement, attempt payment settlement.
        //
        // Payment processing is separated from the order-placement
        // database transaction.
        return OrderResponse.from(
                paymentSettlementService.settleCharge(orderId, accessToken)
        );
    }

    /*
     * PAY AN EXISTING ORDER
     *
     * This is useful when an earlier payment attempt did not complete
     * and the customer needs to retry.
     */
    public OrderResponse payOrder(
            Long orderId,
            Long callerUserId,
            List<Long> callerOrgIds,
            String accessToken
    ) {

        CommerceOrder order = requireOrder(orderId);
        requireOrderVisible(order, callerUserId, callerOrgIds);

        // Only the customer who owns this order may pay it.
        if (!order.getCustomerUserId().equals(callerUserId)) {
            throw ApiException.forbidden("Only the customer can pay this order");
        }

        // Payment retries are allowed only for these two states.
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                && order.getStatus() != OrderStatus.ACTION_REQUIRED) {

            throw ApiException.conflict(
                    "PAY_NOT_ALLOWED",
                    "Order cannot be paid in status " + order.getStatus()
            );
        }

        // Delegate the actual payment operation.
        return OrderResponse.from(
                paymentSettlementService.settleCharge(orderId, accessToken)
        );
    }

    /*
     * GET ONE ORDER
     *
     * readOnly = true tells Spring this transaction is intended
     * only for reading data.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(
            Long orderId,
            Long callerUserId,
            List<Long> callerOrgIds
    ) {

        CommerceOrder order = requireOrder(orderId);
        requireOrderVisible(order, callerUserId, callerOrgIds);

        return OrderResponse.from(order);
    }

    /*
     * LIST ORDERS
     *
     * mineOnly determines whether to return:
     *
     * true  -> only orders belonging to the current customer.
     * false -> all orders belonging to the organisation.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(
            Long organisationId,
            Long callerUserId,
            List<Long> callerOrgIds,
            boolean mineOnly
    ) {

        requireOrgAccess(callerOrgIds, organisationId);

        List<CommerceOrder> orders = mineOnly
                ? orderRepository.findByOrganisationIdAndCustomerUserIdOrderByCreatedAtDesc(
                organisationId,
                callerUserId
        )
                : orderRepository.findByOrganisationIdOrderByCreatedAtDesc(
                organisationId
        );

        // Convert each entity into an API response DTO.
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * Customer order history for every order this user placed (marketplace).
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> listCustomerOrders(Long callerUserId) {
        return orderRepository.findByCustomerUserIdOrderByCreatedAtDesc(callerUserId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    /*
     * CANCEL ORDER
     *
     * A confirmed order has already been paid, so a refund must be
     * attempted before finalising cancellation.
     */
    public OrderResponse cancelOrder(
            Long orderId,
            Long callerUserId,
            List<Long> callerOrgIds,
            String accessToken
    ) {

        CommerceOrder order = requireOrder(orderId);
        requireOrderVisible(order, callerUserId, callerOrgIds);

        // Only the customer who placed the order may cancel it.
        if (!order.getCustomerUserId().equals(callerUserId)) {
            throw ApiException.forbidden("Only the customer can cancel this order");
        }

        // Reject cancellation for states outside the allowed set.
        if (!CANCELLABLE.contains(order.getStatus())) {
            throw ApiException.conflict(
                    "CANCEL_NOT_ALLOWED",
                    "Order cannot be cancelled in status " + order.getStatus()
            );
        }

        // A confirmed order has already completed payment.
        // Attempt the refund before finalising cancellation.
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            paymentSettlementService.settleRefund(order, accessToken);
        }

        // Delegate stock release and cancellation finalisation.
        return orderCancelService.finalizeCancel(orderId);
    }

    /*
     * START FULFILMENT
     *
     * This represents the point where the organisation begins
     * preparing a confirmed order for delivery.
     */
    @Transactional
    public OrderResponse startFulfilment(
            Long orderId,
            List<Long> callerOrgIds
    ) {

        CommerceOrder order = requireOrder(orderId);

        requireOrgAccess(callerOrgIds, order.getOrganisationId());

        // Only a confirmed order can enter fulfilment.
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw ApiException.conflict(
                    "FULFIL_NOT_ALLOWED",
                    "Only CONFIRMED orders can enter fulfilment"
            );
        }

        // Change the domain state.
        order.transitionTo(OrderStatus.FULFILMENT_PENDING);

        // Save the updated order.
        CommerceOrder saved = orderRepository.save(order);

        // Initialise/load the items collection while the transaction
        // is active, so response mapping can access it.
        saved.getItems().size();

        return OrderResponse.from(saved);
    }

    /*
     * MARK ORDER AS FULFILLED
     *
     * Called after the order has been prepared and fulfilled.
     */
    @Transactional
    public OrderResponse markFulfilled(
            Long orderId,
            List<Long> callerOrgIds
    ) {

        CommerceOrder order = requireOrder(orderId);

        requireOrgAccess(callerOrgIds, order.getOrganisationId());

        // Both CONFIRMED and FULFILMENT_PENDING are accepted here.
        if (order.getStatus() != OrderStatus.FULFILMENT_PENDING
                && order.getStatus() != OrderStatus.CONFIRMED) {

            throw ApiException.conflict(
                    "FULFIL_NOT_ALLOWED",
                    "Order cannot be fulfilled from " + order.getStatus()
            );
        }

        // Apply the domain state transition.
        order.transitionTo(OrderStatus.FULFILLED);

        CommerceOrder saved = orderRepository.save(order);

        // Initialise the items collection before mapping the response.
        saved.getItems().size();

        // Publish a domain event for downstream consumers.
        eventPublisher.publish(
                CommerceEvents.of(saved, CommerceEventType.ORDER_FULFILLED)
        );

        return OrderResponse.from(saved);
    }

    /*
     * COMMON HELPER
     *
     * Avoids repeating the same order lookup and not-found handling
     * in every public method.
     */
    private CommerceOrder requireOrder(Long orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound(
                        "ORDER_NOT_FOUND",
                        "Order not found: " + orderId
                ));
    }

    private static void requireOrderVisible(
            CommerceOrder order,
            Long callerUserId,
            List<Long> callerOrgIds
    ) {
        if (callerUserId != null && callerUserId.equals(order.getCustomerUserId())) {
            return;
        }
        requireOrgAccess(callerOrgIds, order.getOrganisationId());
    }

    private Long resolveSellerFromItems(Long explicitOrganisationId, List<OrderItemRequest> items) {
        if (explicitOrganisationId != null) {
            for (OrderItemRequest line : items) {
                Product product = productRepository.findById(line.productId())
                        .orElseThrow(() -> ApiException.notFound(
                                "PRODUCT_NOT_FOUND",
                                "Product not found: " + line.productId()
                        ));
                if (!product.getOrganisationId().equals(explicitOrganisationId)) {
                    throw ApiException.unprocessable(
                            "PRODUCT_ORG_MISMATCH",
                            "Product " + line.productId() + " is not sold by seller " + explicitOrganisationId
                    );
                }
            }
            return explicitOrganisationId;
        }
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("At least one order line is required");
        }
        Long seller = null;
        for (OrderItemRequest line : items) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> ApiException.notFound(
                            "PRODUCT_NOT_FOUND",
                            "Product not found: " + line.productId()
                    ));
            Long productSeller = product.getOrganisationId();
            if (seller == null) {
                seller = productSeller;
            } else if (!seller.equals(productSeller)) {
                throw ApiException.unprocessable(
                        "MULTI_SELLER_ORDER",
                        "All items must be from the same seller"
                );
            }
        }
        return seller;
    }

    /*
     * COMMON ORGANISATION ACCESS CHECK
     *
     * callerOrgIds represents the organisations the authenticated
     * caller is allowed to access.
     */
    private static void requireOrgAccess(
            List<Long> callerOrgIds,
            Long organisationId
    ) {

        if (callerOrgIds == null || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden(
                    "No access to organisation " + organisationId
            );
        }
    }
}
