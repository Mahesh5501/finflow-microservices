package com.finflow.commerce.service;

import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderItem;
import com.finflow.commerce.entity.OrderStatus;
import com.finflow.commerce.entity.PaymentOutbox;
import com.finflow.commerce.entity.Product;
import com.finflow.commerce.event.CommerceEventPublisher;
import com.finflow.commerce.event.CommerceEventType;
import com.finflow.commerce.event.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.payment.PaymentChargeRequest;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.payment.PaymentPort;
import com.finflow.commerce.payment.PaymentRefundRequest;
import com.finflow.commerce.payment.PaymentResult;
import com.finflow.commerce.repository.CommerceOrderRepository;
import com.finflow.commerce.repository.PaymentOutboxRepository;
import com.finflow.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Wallet/PSP calls happen outside the order-placement TX.
 * DB updates for order + outbox run in a short follow-up TX via TransactionTemplate.
 */
@Service
public class PaymentSettlementService {

    private final CommerceOrderRepository orderRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final ProductRepository productRepository;
    private final PaymentPort paymentPort;
    private final TransactionTemplate transactionTemplate;
    private final CommerceEventPublisher eventPublisher;

    public PaymentSettlementService(
            CommerceOrderRepository orderRepository,
            PaymentOutboxRepository outboxRepository,
            ProductRepository productRepository,
            PaymentPort paymentPort,
            TransactionTemplate transactionTemplate,
            CommerceEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.productRepository = productRepository;
        this.paymentPort = paymentPort;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    public CommerceOrder settleCharge(Long orderId, String accessToken) {
        CommerceOrder snapshot = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found: " + orderId));
        if (snapshot.getStatus() != OrderStatus.PENDING_PAYMENT && snapshot.getStatus() != OrderStatus.ACTION_REQUIRED) {
            return transactionTemplate.execute(status -> {
                CommerceOrder managed = orderRepository.findById(orderId).orElseThrow();
                managed.getItems().size();
                return managed;
            });
        }

        PaymentOutbox outbox = outboxRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.conflict("OUTBOX_MISSING", "Payment outbox missing for order " + orderId));

        PaymentResult payment = paymentPort.charge(new PaymentChargeRequest(
                snapshot.getCustomerUserId(),
                snapshot.getTotalAmount(),
                snapshot.getCurrencyCode(),
                "commerce-pay-" + snapshot.getOrderId(),
                accessToken,
                parseHint(outbox.getSandboxHint())
        ));

        CommerceOrder settled = transactionTemplate.execute(status -> applyChargeResult(orderId, payment));
        if (settled != null && settled.getStatus() == OrderStatus.CONFIRMED) {
            eventPublisher.publish(CommerceEvents.of(settled, CommerceEventType.ORDER_PAID));
        }
        return settled;
    }

    public void settleRefund(CommerceOrder order, String accessToken) {
        PaymentResult refund = paymentPort.refund(new PaymentRefundRequest(
                order.getCustomerUserId(),
                order.getPaymentWalletId(),
                order.getTotalAmount(),
                "commerce-refund-" + order.getOrderId(),
                accessToken
        ));
        if (refund.outcome() != PaymentOutcome.APPROVED) {
            throw ApiException.conflict(
                    "REFUND_FAILED",
                    "Wallet refund failed: " + (refund.failureCode() == null ? refund.outcome() : refund.failureCode())
            );
        }
    }

    private CommerceOrder applyChargeResult(Long orderId, PaymentResult payment) {
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found: " + orderId));
        PaymentOutbox outbox = outboxRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.conflict("OUTBOX_MISSING", "Payment outbox missing for order " + orderId));

        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            return order;
        }

        outbox.markProcessing();
        switch (payment.outcome()) {
            case APPROVED -> {
                order.recordPayment(payment.walletId(), payment.walletTransactionId());
                order.transitionTo(OrderStatus.CONFIRMED);
                outbox.markDone();
            }
            case DECLINED -> {
                releaseStock(order);
                order.transitionTo(OrderStatus.PAYMENT_FAILED);
                outbox.markFailed(payment.failureCode() == null ? "DECLINED" : payment.failureCode());
            }
            case UNCERTAIN -> {
                order.transitionTo(OrderStatus.ACTION_REQUIRED);
                outbox.markPending(payment.failureCode() == null ? "UNCERTAIN" : payment.failureCode());
            }
        }
        CommerceOrder saved = orderRepository.save(order);
        // Initialize lazy items before TX ends (OrderResponse maps outside).
        saved.getItems().size();
        return saved;
    }

    private void releaseStock(CommerceOrder order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product missing during stock release"));
            product.releaseStock(item.getQuantity());
        }
    }

    private static PaymentOutcome parseHint(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PaymentOutcome.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
