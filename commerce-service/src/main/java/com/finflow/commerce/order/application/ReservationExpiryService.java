package com.finflow.commerce.order.application;

import com.finflow.commerce.order.domain.CommerceOrder;
import com.finflow.commerce.order.domain.OrderItem;
import com.finflow.commerce.order.domain.OrderStatus;
import com.finflow.commerce.catalogue.domain.Product;
import com.finflow.commerce.integration.event.CommerceEventPublisher;
import com.finflow.commerce.integration.event.CommerceEventType;
import com.finflow.commerce.order.application.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.order.persistence.CommerceOrderRepository;
import com.finflow.commerce.order.persistence.PaymentOutboxRepository;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Releases stock for unpaid orders that have exceeded their reservation TTL.
 *
 * Eligible orders:
 * PENDING_PAYMENT
 * ACTION_REQUIRED
 *
 * Order application maintenance boundary using order/product/payment-work
 * repositories and the event publisher. No scheduler currently calls it automatically;
 * tests/operators may supply a cutoff explicitly. @Service makes it injectable and
 * @Transactional processes the selected batch atomically, joining a caller transaction
 * when present. Removing it would separate stock, order and work state writes.
 * An exception rolls back the batch; per-order transactions would change that behavior.
 * Expiry releases PENDING_PAYMENT/ACTION_REQUIRED stock, marks unfinished work failed
 * and publishes ORDER_EXPIRED inside the transaction. A payment result may arrive late:
 * the current workflow lacks order-level coordination/reconciliation for that race.
 */
@Service
public class ReservationExpiryService {

    // Logger for recording the result of scheduled expiry processing.
    private static final Logger log =
            LoggerFactory.getLogger(ReservationExpiryService.class);

    // Only these statuses represent orders that can still hold
    // an unpaid stock reservation.
    private static final EnumSet<OrderStatus> RESERVING = EnumSet.of(
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.ACTION_REQUIRED
    );

    private final CommerceOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final CommerceEventPublisher eventPublisher;

    public ReservationExpiryService(
            CommerceOrderRepository orderRepository,
            ProductRepository productRepository,
            PaymentOutboxRepository outboxRepository,
            CommerceEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Finds and expires unpaid orders created before the supplied cutoff.
     *
     * @return number of orders processed as expired
     */
    @Transactional
    public int expireStaleReservations(Instant cutoff) {

        // Find orders whose status indicates an active payment reservation
        // and whose creation time is older than the cutoff.
        List<CommerceOrder> stale =
                orderRepository.findByStatusInAndCreatedAtBefore(
                        RESERVING,
                        cutoff
                );

        int count = 0;

        // Process each stale order.
        for (CommerceOrder order : stale) {
            expireOne(order);
            count++;
        }

        // Log only when at least one order was expired.
        if (count > 0) {
            log.info(
                    "Expired {} unpaid order reservation(s) older than {}",
                    count,
                    cutoff
            );
        }

        return count;
    }

    /**
     * Expires one order and releases its reserved stock.
     *
     * This method runs inside the transaction opened by
     * expireStaleReservations().
     */
    private void expireOne(CommerceOrder order) {

        // Restore the stock reserved for every order item.
        for (OrderItem item : order.getItems()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound(
                            "PRODUCT_NOT_FOUND",
                            "Product missing during expiry"
                    ));

            product.releaseStock(item.getQuantity());
        }

        // Change the order state to EXPIRED.
        order.transitionTo(OrderStatus.EXPIRED);

        // Persist the order status change.
        orderRepository.save(order);

        // Find the payment outbox record, if one exists.
        outboxRepository.findByOrderId(order.getOrderId())
                .ifPresent(outbox -> {

                    // A completed payment operation must not be overwritten.
                    if (outbox.getStatus()
                            != com.finflow.commerce.order.domain.OutboxStatus.DONE) {

                        // Mark the payment operation as failed because
                        // the reservation expired.
                        outbox.markFailed("RESERVATION_TTL_EXPIRED");

                        outboxRepository.save(outbox);
                    }
                });

        // Notify consumers that this order expired.
        // Note: this publication currently happens before the surrounding
        // database transaction commits.
        eventPublisher.publish(
                CommerceEvents.of(order, CommerceEventType.ORDER_EXPIRED)
        );
    }
}