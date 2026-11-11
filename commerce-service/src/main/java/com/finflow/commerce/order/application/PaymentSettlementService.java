package com.finflow.commerce.order.application;

import com.finflow.commerce.order.domain.CommerceOrder;
import com.finflow.commerce.order.domain.OrderItem;
import com.finflow.commerce.order.domain.OrderStatus;
import com.finflow.commerce.order.domain.PaymentOutbox;
import com.finflow.commerce.catalogue.domain.Product;
import com.finflow.commerce.integration.event.CommerceEventPublisher;
import com.finflow.commerce.integration.event.CommerceEventType;
import com.finflow.commerce.order.application.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.payment.PaymentChargeRequest;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.payment.PaymentGateway;
import com.finflow.commerce.payment.PaymentRefundRequest;
import com.finflow.commerce.payment.PaymentResult;
import com.finflow.commerce.order.persistence.CommerceOrderRepository;
import com.finflow.commerce.order.persistence.PaymentOutboxRepository;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Handles communication with the payment provider and applies its result
 * to the Commerce Service database.
 *
 * External payment calls happen outside the database transaction.
 * Order and outbox updates happen in a short follow-up transaction.
 *
 * Order application coordinator called by CommerceService and reused by
 * ReturnService for refunds. It depends on PaymentGateway, never wallet persistence.
 * Order/outbox/product repositories apply local state; TransactionTemplate defines
 * short REQUIRED transactions; CommerceEventPublisher emits the resulting paid event.
 * Constructor injection expresses dependency inversion: swapping the provider leaves
 * the order workflow unchanged. @Service is needed for injection; an explicit @Bean
 * is the alternative. Avoid a class-wide @Transactional annotation around HTTP.
 *
 * Charge flow: read detached order/work -> HTTP with stable commerce-pay-{orderId}
 * key -> transaction reloads and applies result -> publish ORDER_PAID after callback.
 * APPROVED confirms; DECLINED releases stock; UNCERTAIN retains stock and payment work.
 * TransactionTemplate avoids self-invocation problems with a private result method.
 * It joins an ambient transaction, so callers must keep the documented no-outer-TX
 * contract. Removing the template loses atomic order/outbox/stock settlement.
 *
 * Refund uses commerce-refund-{orderId}; only APPROVED lets cancel/return continue.
 * No refund-work ledger or automatic payment-work drain exists. Settlement versus
 * cancellation/expiry races remain limitations; @Version on Product alone does not
 * serialize order state changes or undo an already successful external debit.
 */
@Service
public class PaymentSettlementService {

    // Loads and updates commerce orders.
    private final CommerceOrderRepository orderRepository;

    // Tracks the payment operation associated with an order.
    private final PaymentOutboxRepository outboxRepository;

    // Used to release reserved stock when payment is declined.
    private final ProductRepository productRepository;

    // Abstraction for the payment provider.
    // The actual implementation may use a wallet service or simulator.
    private final PaymentGateway paymentPort;

    // Allows us to explicitly define short database transaction boundaries.
    private final TransactionTemplate transactionTemplate;

    // Publishes the ORDER_PAID event after successful settlement.
    private final CommerceEventPublisher eventPublisher;

    // Constructor injection makes all dependencies explicit.
    public PaymentSettlementService(
            CommerceOrderRepository orderRepository,
            PaymentOutboxRepository outboxRepository,
            ProductRepository productRepository,
            PaymentGateway paymentPort,
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

    /**
     * Attempts to charge the customer for an order.
     */
    public CommerceOrder settleCharge(Long orderId, String accessToken) {

        // Read the order before calling the external payment service.
        // This is a detached/read snapshot, not a long-running DB transaction.
        CommerceOrder snapshot = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound(
                        "ORDER_NOT_FOUND",
                        "Order not found: " + orderId
                ));

        // Only unpaid orders that are awaiting payment or require action
        // should proceed to the payment provider.
        if (snapshot.getStatus() != OrderStatus.PENDING_PAYMENT
                && snapshot.getStatus() != OrderStatus.ACTION_REQUIRED) {

            // The order is already in another state.
            // Return the latest database representation instead of charging.
            return transactionTemplate.execute(status -> {
                CommerceOrder managed = orderRepository.findById(orderId)
                        .orElseThrow();

                // Initialize lazy-loaded items while the transaction is active.
                // Response mapping may access these after this method returns.
                managed.getItems().size();

                return managed;
            });
        }

        // The payment outbox stores information associated with the
        // payment attempt, including the simulator hint.
        PaymentOutbox outbox = outboxRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.conflict(
                        "OUTBOX_MISSING",
                        "Payment outbox missing for order " + orderId
                ));

        // IMPORTANT:
        // This is an external service call.
        // It is intentionally outside the database transaction.
        //
        // The idempotency key is stable for this order so retries can be
        // recognized by the payment provider.
        PaymentResult payment = paymentPort.charge(
                new PaymentChargeRequest(
                        snapshot.getCustomerUserId(),
                        snapshot.getTotalAmount(),
                        snapshot.getCurrencyCode(),
                        "commerce-pay-" + snapshot.getOrderId(),
                        accessToken,
                        parseHint(outbox.getSandboxHint())
                )
        );

        // Apply the provider's result in a short database transaction.
        CommerceOrder settled = transactionTemplate.execute(
                status -> applyChargeResult(orderId, payment)
        );

        // Publish ORDER_PAID only when the resulting order is confirmed.
        // Note: publication is outside the transaction callback, but this
        // implementation still does not provide durable event delivery.
        if (settled != null && settled.getStatus() == OrderStatus.CONFIRMED) {
            eventPublisher.publish(
                    CommerceEvents.of(settled, CommerceEventType.ORDER_PAID)
            );
        }

        return settled;
    }

    /**
     * Refunds the customer for the supplied order.
     *
     * The refund is an external operation and is not part of a local
     * database transaction.
     */
    public void settleRefund(CommerceOrder order, String accessToken) {

        PaymentResult refund = paymentPort.refund(
                new PaymentRefundRequest(
                        order.getCustomerUserId(),
                        order.getPaymentWalletId(),
                        order.getTotalAmount(),
                        "commerce-refund-" + order.getOrderId(),
                        accessToken
                )
        );

        // A refund is considered successful only when the provider
        // explicitly reports APPROVED.
        if (refund.outcome() != PaymentOutcome.APPROVED) {
            throw ApiException.conflict(
                    "REFUND_FAILED",
                    "Wallet refund failed: "
                            + (refund.failureCode() == null
                            ? refund.outcome()
                            : refund.failureCode())
            );
        }
    }

    /**
     * Applies the external payment result to the order and payment outbox.
     *
     * This method is called inside TransactionTemplate's transaction.
     */
    private CommerceOrder applyChargeResult(
            Long orderId,
            PaymentResult payment
    ) {

        // Reload the order inside the transaction.
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound(
                        "ORDER_NOT_FOUND",
                        "Order not found: " + orderId
                ));

        // Load the corresponding payment outbox record.
        PaymentOutbox outbox = outboxRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.conflict(
                        "OUTBOX_MISSING",
                        "Payment outbox missing for order " + orderId
                ));

        // Avoid applying another result if the order has already reached
        // one of these final payment states.
        if (order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            return order;
        }

        // Mark the payment operation as being processed.
        outbox.markProcessing();

        // Different provider outcomes require different business actions.
        switch (payment.outcome()) {

            case APPROVED -> {
                // Save the wallet and transaction references returned
                // by the payment provider.
                // Simulated approval moves no wallet money and has no references.
                // The explicit provider marker avoids inventing ledger IDs or
                // relaxing recordPayment's validation for a real wallet result.
                if (!payment.simulated()) {
                    order.recordPayment(payment.walletId(), payment.walletTransactionId());
                }

                // Payment succeeded: confirm the order.
                order.transitionTo(OrderStatus.CONFIRMED);

                // Mark the payment outbox operation as complete.
                outbox.markDone();
            }

            case DECLINED -> {
                // Payment failed: release the stock reserved during checkout.
                releaseStock(order);

                // Mark the order as payment failed.
                order.transitionTo(OrderStatus.PAYMENT_FAILED);

                // Record why the payment operation failed.
                outbox.markFailed(
                        payment.failureCode() == null
                                ? "DECLINED"
                                : payment.failureCode()
                );
            }

            case UNCERTAIN -> {
                // We do not know whether the provider processed the payment.
                // Do not release stock or assume that payment failed.
                // A replay or /pay retry can be uncertain again. The domain state
                // machine rejects ACTION_REQUIRED -> ACTION_REQUIRED, so retain the
                // existing state while recording the latest pending work result.
                if (order.getStatus() != OrderStatus.ACTION_REQUIRED) {
                    order.transitionTo(OrderStatus.ACTION_REQUIRED);
                }

                // Keep the operation pending for further investigation/retry.
                outbox.markPending(
                        payment.failureCode() == null
                                ? "UNCERTAIN"
                                : payment.failureCode()
                );
            }
        }

        // Persist the order changes.
        // The outbox is also a managed entity and its changes are persisted
        // through JPA dirty checking within this transaction.
        CommerceOrder saved = orderRepository.save(order);

        // Initialize lazy-loaded items before the transaction ends.
        saved.getItems().size();

        return saved;
    }

    /**
     * Restores the stock reserved for every item in the order.
     */
    private void releaseStock(CommerceOrder order) {

        for (OrderItem item : order.getItems()) {

            // Load the product associated with the order item.
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound(
                            "PRODUCT_NOT_FOUND",
                            "Product missing during stock release"
                    ));

            // Restore the reserved quantity.
            product.releaseStock(item.getQuantity());
        }
    }

    /**
     * Converts a stored simulator hint into a PaymentOutcome.
     *
     * Invalid or missing hints are treated as null.
     */
    private static PaymentOutcome parseHint(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return PaymentOutcome.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            // An invalid hint should not crash payment settlement.
            return null;
        }
    }
}
