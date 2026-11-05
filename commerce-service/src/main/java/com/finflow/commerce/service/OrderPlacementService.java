package com.finflow.commerce.service;

import com.finflow.commerce.dto.OrderItemRequest;
import com.finflow.commerce.dto.ShippingAddressRequest;
import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.Coupon;
import com.finflow.commerce.entity.IdempotencyRecord;
import com.finflow.commerce.entity.OrderItem;
import com.finflow.commerce.entity.OrderStatus;
import com.finflow.commerce.entity.PaymentOutbox;
import com.finflow.commerce.entity.Product;
import com.finflow.commerce.entity.ProductStatus;
import com.finflow.commerce.entity.ShippingAddress;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.repository.CommerceOrderRepository;
import com.finflow.commerce.repository.CouponRepository;
import com.finflow.commerce.repository.IdempotencyRecordRepository;
import com.finflow.commerce.repository.PaymentOutboxRepository;
import com.finflow.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates PENDING_PAYMENT orders + outbox rows in one DB transaction (no wallet HTTP).
 */
@Service
public class OrderPlacementService {

    private final ProductRepository productRepository;
    private final CommerceOrderRepository orderRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final CouponRepository couponRepository;

    public OrderPlacementService(
            ProductRepository productRepository,
            CommerceOrderRepository orderRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            PaymentOutboxRepository outboxRepository,
            CouponRepository couponRepository
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.outboxRepository = outboxRepository;
        this.couponRepository = couponRepository;
    }

    @Transactional
    public Long placePendingOrder(
            Long customerUserId,
            List<Long> callerOrgIds,
            String idempotencyKey,
            Long organisationId,
            List<OrderItemRequest> items,
            ShippingAddressRequest shippingAddress,
            String couponCode,
            PaymentOutcome sandboxHint
    ) {
        requireOrgAccess(callerOrgIds, organisationId);
        requireIdempotencyKey(idempotencyKey);

        var existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existing.isPresent()) {
            return existing.get().getOrderId();
        }

        CommerceOrder order = new CommerceOrder(organisationId, customerUserId, "INR");
        if (shippingAddress != null) {
            order.setShippingAddress(toAddress(shippingAddress));
        }

        for (OrderItemRequest line : items) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product not found: " + line.productId()));
            if (!product.getOrganisationId().equals(organisationId)) {
                throw ApiException.unprocessable("PRODUCT_ORG_MISMATCH", "Product belongs to another organisation");
            }
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw ApiException.unprocessable("PRODUCT_INACTIVE", "Product is not active: " + line.productId());
            }
            try {
                product.reserveStock(line.quantity());
            } catch (IllegalStateException ex) {
                throw ApiException.conflict("INSUFFICIENT_STOCK", "Not enough stock for product " + line.productId());
            }
            order.addItem(new OrderItem(
                    product.getProductId(),
                    product.getName(),
                    product.getUnitPrice(),
                    line.quantity()
            ));
        }

        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponRepository.findByOrganisationIdAndCodeIgnoreCase(organisationId, couponCode.trim())
                    .orElseThrow(() -> ApiException.notFound("COUPON_NOT_FOUND", "Coupon not found: " + couponCode));
            try {
                order.applyDiscount(coupon.getCode(), coupon.apply(order.getTotalAmount()));
            } catch (IllegalStateException ex) {
                throw ApiException.unprocessable("COUPON_INVALID", ex.getMessage());
            }
        }

        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        order = orderRepository.save(order);
        idempotencyRecordRepository.save(new IdempotencyRecord(
                idempotencyKey.trim(), organisationId, order.getOrderId()
        ));
        outboxRepository.save(new PaymentOutbox(
                order.getOrderId(),
                sandboxHint == null ? null : sandboxHint.name()
        ));
        return order.getOrderId();
    }

    private static ShippingAddress toAddress(ShippingAddressRequest request) {
        return new ShippingAddress(
                request.line1().trim(),
                request.line2() == null ? null : request.line2().trim(),
                request.city().trim(),
                request.state() == null ? null : request.state().trim(),
                request.postalCode().trim(),
                request.country()
        );
    }

    private static void requireOrgAccess(List<Long> callerOrgIds, Long organisationId) {
        if (callerOrgIds == null || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden("No access to organisation " + organisationId);
        }
    }

    private static void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("Idempotency-Key header is required");
        }
    }
}
