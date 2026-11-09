
package com.finflow.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/**
 * Transactional outbox for order payment settlement.
 *
 * The outbox record is written in the same database transaction as the order.
 * Payment processing can then happen after that transaction commits.
 *
 * This avoids making a wallet HTTP call while the order-creation transaction
 * is still holding database resources.
 */
@Getter
@Entity
@Table(name = "payment_outbox")
public class PaymentOutbox {

    // Database-generated primary key for this outbox record.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The order for which payment settlement must be processed.
    // unique = true ensures only one outbox record exists per order.
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    // Current processing state of this outbox record.
    // EnumType.STRING stores values such as PENDING and DONE,
    // rather than fragile numeric enum positions.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    // Number of times processing has been attempted.
    @Column(nullable = false)
    private int attempts;

    // Most recent processing error, limited to 500 characters.
    @Column(name = "last_error", length = 500)
    private String lastError;

    // Optional payment simulation instruction.
    // Used by the simulated payment adapter in sandbox scenarios.
    @Column(name = "sandbox_hint", length = 20)
    private String sandboxHint;

    // Time at which this outbox record was created.
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Time at which the outbox record was last updated.
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Required by JPA when loading an existing database row.
    protected PaymentOutbox() {
    }

    // Creates a new outbox record for an order.
    public PaymentOutbox(Long orderId, String sandboxHint) {
        this.orderId = orderId;

        // New payment work starts in PENDING state.
        this.status = OutboxStatus.PENDING;

        // No processing attempt has happened yet.
        this.attempts = 0;

        this.sandboxHint = sandboxHint;

        // Use the same timestamp for creation and initial update.
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Marks the record as currently being processed.
    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;

        // Count this processing attempt.
        this.attempts++;

        this.updatedAt = Instant.now();
    }

    // Marks payment processing as successfully completed.
    public void markDone() {
        this.status = OutboxStatus.DONE;

        // A successful result means the previous error is no longer relevant.
        this.lastError = null;

        this.updatedAt = Instant.now();
    }

    // Marks processing as failed.
    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;

        // Store the error safely within the database column's size.
        this.lastError = truncate(error);

        this.updatedAt = Instant.now();
    }

    // Returns the record to PENDING so it can be retried later.
    public void markPending(String error) {
        this.status = OutboxStatus.PENDING;
        this.lastError = truncate(error);
        this.updatedAt = Instant.now();
    }

    // Prevents errors longer than 500 characters from exceeding the column.
    private static String truncate(String error) {
        if (error == null) {
            return null;
        }

        return error.length() <= 500
                ? error
                : error.substring(0, 500);
    }
}
