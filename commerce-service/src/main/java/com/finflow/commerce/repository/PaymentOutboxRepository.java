package com.finflow.commerce.repository;

import com.finflow.commerce.entity.OutboxStatus;
import com.finflow.commerce.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Database access for payment outbox records.
 *
 * Each payment outbox record is associated with an order.
 */
public interface PaymentOutboxRepository
        extends JpaRepository<PaymentOutbox, Long> {

    /**
     * Finds the payment outbox record associated with an order.
     *
     * Optional is used because the record might not exist.
     */
    Optional<PaymentOutbox> findByOrderId(Long orderId);

    /**
     * Retrieves at most 50 outbox records with the requested status,
     * ordered oldest first.
     *
     * Intended for processing a limited batch of pending operations.
     */
    List<PaymentOutbox> findTop50ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}