package com.finflow.commerce.service;

import com.finflow.commerce.dto.CouponRequest;
import com.finflow.commerce.dto.CouponResponse;
import com.finflow.commerce.dto.CreateOrderRequest;
import com.finflow.commerce.dto.OrderResponse;
import com.finflow.commerce.dto.ProductRequest;
import com.finflow.commerce.dto.ProductResponse;
import com.finflow.commerce.dto.ProductUpdateRequest;
import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.Coupon;
import com.finflow.commerce.entity.OrderStatus;
import com.finflow.commerce.event.CommerceEventPublisher;
import com.finflow.commerce.event.CommerceEventType;
import com.finflow.commerce.event.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.repository.CommerceOrderRepository;
import com.finflow.commerce.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

/**
 * Order orchestration (+ thin product delegates for existing callers/tests).
 * Placement TX reserves stock; {@link PaymentSettlementService} charges after commit.
 */
@Service
public class CommerceService {

    private static final EnumSet<OrderStatus> CANCELLABLE = EnumSet.of(
            OrderStatus.CREATED,
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.ACTION_REQUIRED,
            OrderStatus.CONFIRMED
    );

    private final CatalogueService catalogueService;
    private final CommerceOrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final OrderPlacementService orderPlacementService;
    private final PaymentSettlementService paymentSettlementService;
    private final OrderCancelService orderCancelService;
    private final CommerceEventPublisher eventPublisher;

    public CommerceService(
            CatalogueService catalogueService,
            CommerceOrderRepository orderRepository,
            CouponRepository couponRepository,
            OrderPlacementService orderPlacementService,
            PaymentSettlementService paymentSettlementService,
            OrderCancelService orderCancelService,
            CommerceEventPublisher eventPublisher
    ) {
        this.catalogueService = catalogueService;
        this.orderRepository = orderRepository;
        this.couponRepository = couponRepository;
        this.orderPlacementService = orderPlacementService;
        this.paymentSettlementService = paymentSettlementService;
        this.orderCancelService = orderCancelService;
        this.eventPublisher = eventPublisher;
    }

    public ProductResponse createProduct(Long callerUserId, List<Long> callerOrgIds, ProductRequest request) {
        return catalogueService.createProduct(callerOrgIds, request);
    }

    public List<ProductResponse> listProducts(Long organisationId, List<Long> callerOrgIds) {
        return catalogueService.listProducts(organisationId, callerOrgIds);
    }

    public ProductResponse updateProduct(Long productId, List<Long> callerOrgIds, ProductUpdateRequest request) {
        return catalogueService.updateProduct(productId, callerOrgIds, request);
    }

    @Transactional
    public CouponResponse createCoupon(List<Long> callerOrgIds, CouponRequest request) {
        requireOrgAccess(callerOrgIds, request.organisationId());
        if (couponRepository.findByOrganisationIdAndCodeIgnoreCase(request.organisationId(), request.code()).isPresent()) {
            throw ApiException.conflict("COUPON_EXISTS", "Coupon code already exists");
        }
        Coupon coupon = new Coupon(
                request.organisationId(),
                request.code(),
                request.percentOff(),
                request.expiresAt()
        );
        return CouponResponse.from(couponRepository.save(coupon));
    }

    public OrderResponse createOrder(
            Long customerUserId,
            List<Long> callerOrgIds,
            String idempotencyKey,
            String accessToken,
            CreateOrderRequest request
    ) {
        Long orderId = orderPlacementService.placePendingOrder(
                customerUserId,
                callerOrgIds,
                idempotencyKey,
                request.organisationId(),
                request.items(),
                request.shippingAddress(),
                request.couponCode(),
                request.paymentOutcome()
        );
        return OrderResponse.from(paymentSettlementService.settleCharge(orderId, accessToken));
    }

    public OrderResponse payOrder(
            Long orderId,
            Long callerUserId,
            List<Long> callerOrgIds,
            String accessToken
    ) {
        CommerceOrder order = requireOrder(orderId);
        requireOrgAccess(callerOrgIds, order.getOrganisationId());
        if (!order.getCustomerUserId().equals(callerUserId)) {
            throw ApiException.forbidden("Only the customer can pay this order");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.ACTION_REQUIRED) {
            throw ApiException.conflict("PAY_NOT_ALLOWED", "Order cannot be paid in status " + order.getStatus());
        }
        return OrderResponse.from(paymentSettlementService.settleCharge(orderId, accessToken));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, List<Long> callerOrgIds) {
        CommerceOrder order = requireOrder(orderId);
        requireOrgAccess(callerOrgIds, order.getOrganisationId());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(
            Long organisationId,
            Long callerUserId,
            List<Long> callerOrgIds,
            boolean mineOnly
    ) {
        requireOrgAccess(callerOrgIds, organisationId);
        List<CommerceOrder> orders = mineOnly
                ? orderRepository.findByOrganisationIdAndCustomerUserIdOrderByCreatedAtDesc(organisationId, callerUserId)
                : orderRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId);
        return orders.stream().map(OrderResponse::from).toList();
    }

    public OrderResponse cancelOrder(
            Long orderId,
            Long callerUserId,
            List<Long> callerOrgIds,
            String accessToken
    ) {
        CommerceOrder order = requireOrder(orderId);
        requireOrgAccess(callerOrgIds, order.getOrganisationId());
        if (!order.getCustomerUserId().equals(callerUserId)) {
            throw ApiException.forbidden("Only the customer can cancel this order");
        }
        if (!CANCELLABLE.contains(order.getStatus())) {
            throw ApiException.conflict("CANCEL_NOT_ALLOWED", "Order cannot be cancelled in status " + order.getStatus());
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            paymentSettlementService.settleRefund(order, accessToken);
        }
        return orderCancelService.finalizeCancel(orderId);
    }

    @Transactional
    public OrderResponse startFulfilment(Long orderId, List<Long> callerOrgIds) {
        CommerceOrder order = requireOrder(orderId);
        requireOrgAccess(callerOrgIds, order.getOrganisationId());
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw ApiException.conflict("FULFIL_NOT_ALLOWED", "Only CONFIRMED orders can enter fulfilment");
        }
        order.transitionTo(OrderStatus.FULFILMENT_PENDING);
        CommerceOrder saved = orderRepository.save(order);
        saved.getItems().size();
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse markFulfilled(Long orderId, List<Long> callerOrgIds) {
        CommerceOrder order = requireOrder(orderId);
        requireOrgAccess(callerOrgIds, order.getOrganisationId());
        if (order.getStatus() != OrderStatus.FULFILMENT_PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw ApiException.conflict("FULFIL_NOT_ALLOWED", "Order cannot be fulfilled from " + order.getStatus());
        }
        order.transitionTo(OrderStatus.FULFILLED);
        CommerceOrder saved = orderRepository.save(order);
        saved.getItems().size();
        eventPublisher.publish(CommerceEvents.of(saved, CommerceEventType.ORDER_FULFILLED));
        return OrderResponse.from(saved);
    }

    private CommerceOrder requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found: " + orderId));
    }

    private static void requireOrgAccess(List<Long> callerOrgIds, Long organisationId) {
        if (callerOrgIds == null || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden("No access to organisation " + organisationId);
        }
    }
}
