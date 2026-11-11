package com.finflow.commerce.order.persistence;

import com.finflow.commerce.order.domain.OutboxStatus;
import com.finflow.commerce.order.domain.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Database access for payment outbox records.
 * Each payment outbox record is associated with an order.
 *
 * Persistence layer interface called by application services. Spring Data discovers
 * it below the application root and supplies a JPA proxy; @Repository is not required
 * on this interface (it is useful on a hand-written DAO for exception translation).
 * Removing JpaRepository inheritance would remove that generated CRUD contract.
 * Derived queries use Java property names, not SQL columns; custom JPQL must do the
 * same. Repositories do not authenticate callers: application services must supply
 * owner/seller constraints and define transactions spanning multiple repositories.
 * Native SQL is an alternative for database-specific operations, at a portability cost.
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