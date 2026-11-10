package com.finflow.commerce.repository.order;

import com.finflow.commerce.entity.order.OutboxStatus;
import com.finflow.commerce.entity.order.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Database access for payment outbox records.
 * Each payment outbox record is associated with an order.
 */
public interface PaymentOutboxRepository
        extends JpaRepository<PaymentOutbox, Long> {

    /**
     * Finds the payment outbox record associated with an order.
     * @param orderId the ID of the order.
     * @return the payment outbox record with the given order ID.
     * Optional is used because the record might not exist.
     */
    Optional<PaymentOutbox> findByOrderId(Long orderId);

    /**
     * Retrieves at most 50 outbox records with the requested status, ordered oldest first. Intended for processing a limited batch of pending operations.
     * @param status the status of the outbox records.
     * @return the outbox records with the requested status, ordered oldest first.
     */
    List<PaymentOutbox> findTop50ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}