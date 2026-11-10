
package com.finflow.commerce.service.order;

import com.finflow.commerce.service.catalogue.CatalogueService;
import com.finflow.commerce.dto.coupon.CouponRequest;
import com.finflow.commerce.dto.coupon.CouponResponse;
import com.finflow.commerce.dto.order.CreateOrderRequest;
import com.finflow.commerce.dto.order.OrderResponse;
import com.finflow.commerce.dto.catalogue.ProductRequest;
import com.finflow.commerce.dto.catalogue.ProductResponse;
import com.finflow.commerce.dto.catalogue.ProductUpdateRequest;
import com.finflow.commerce.entity.order.CommerceOrder;
import com.finflow.commerce.entity.coupon.Coupon;
import com.finflow.commerce.entity.order.OrderStatus;
import com.finflow.commerce.integration.event.CommerceEventPublisher;
import com.finflow.commerce.integration.event.CommerceEventType;
import com.finflow.commerce.integration.event.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.dto.order.OrderItemRequest;
import com.finflow.commerce.entity.catalogue.Product;
import com.finflow.commerce.repository.catalogue.ProductRepository;
import com.finflow.commerce.repository.order.CommerceOrderRepository;
import com.finflow.commerce.repository.coupon.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

/**
 * Main commerce orchestration service.
 *
 * It coordinates catalogue, coupon, order placement,
 * payment settlement, cancellation and fulfilment operations.
 *
 * It does not implement every business operation itself.
 * Instead, it delegates specialised responsibilities to other services.
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
    private final CatalogueService catalogueService;
    private final ProductRepository productRepository;
    private final CommerceOrderRepository orderRepository;
    private final CouponRepository couponRepository;
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
            CatalogueService catalogueService,
            ProductRepository productRepository,
            CommerceOrderRepository orderRepository,
            CouponRepository couponRepository,
            OrderPlacementService orderPlacementService,
            PaymentSettlementService paymentSettlementService,
            OrderCancelService orderCancelService,
            CommerceEventPublisher eventPublisher
    ) {
        this.catalogueService = catalogueService;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.couponRepository = couponRepository;
        this.orderPlacementService = orderPlacementService;
        this.paymentSettlementService = paymentSettlementService;
        this.orderCancelService = orderCancelService;
        this.eventPublisher = eventPublisher;
    }

    /*
     * PRODUCT OPERATIONS
     *
     * These methods are thin delegates to CatalogueService.
     * CommerceService retains these entry points for existing callers.
     */

    public ProductResponse createProduct(
            Long callerUserId,
            List<Long> callerOrgIds,
            ProductRequest request
    ) {
        // CatalogueService owns the actual product-creation logic.
        return catalogueService.createProduct(callerOrgIds, request);
    }

    public List<ProductResponse> listProducts(
            Long organisationId,
            List<Long> callerOrgIds
    ) {
        // Retrieve products belonging to the requested organisation.
        return catalogueService.listProducts(organisationId, callerOrgIds);
    }

    public ProductResponse updateProduct(
            Long productId,
            List<Long> callerOrgIds,
            ProductUpdateRequest request
    ) {
        // Delegate product updates to the catalogue module.
        return catalogueService.updateProduct(productId, callerOrgIds, request);
    }

    /*
     * COUPON CREATION
     *
     * @Transactional means the database operations in this method
     * execute inside a transaction.
     *
     * If an unchecked exception occurs, Spring rolls back the transaction.
     */
    @Transactional
    public CouponResponse createCoupon(
            List<Long> callerOrgIds,
            CouponRequest request
    ) {

        // A caller must belong to the organisation for which the coupon
        // is being created.
        requireOrgAccess(callerOrgIds, request.organisationId());

        // Prevent creating another coupon with the same code
        // within the same organisation.
        if (couponRepository.findByOrganisationIdAndCodeIgnoreCase(
                request.organisationId(),
                request.code()
        ).isPresent()) {

            throw ApiException.conflict(
                    "COUPON_EXISTS",
                    "Coupon code already exists"
            );
        }

        // Convert the incoming request into a JPA entity.
        Coupon coupon = new Coupon(
                request.organisationId(),
                request.code(),
                request.percentOff(),
                request.expiresAt()
        );

        // Persist the coupon and convert the saved entity into a response.
        return CouponResponse.from(couponRepository.save(coupon));
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
