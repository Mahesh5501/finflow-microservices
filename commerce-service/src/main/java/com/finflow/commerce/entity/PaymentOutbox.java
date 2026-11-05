package com.finflow.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Transactional outbox for order payment settlement.
 * Written in the same TX as the order; drained after commit (no wallet HTTP inside the order TX).
 */
@Entity
@Table(name = "payment_outbox")
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "sandbox_hint", length = 20)
    private String sandboxHint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentOutbox() {
    }

    public PaymentOutbox(Long orderId, String sandboxHint) {
        this.orderId = orderId;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.sandboxHint = sandboxHint;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
        this.attempts++;
        this.updatedAt = Instant.now();
    }

    public void markDone() {
        this.status = OutboxStatus.DONE;
        this.lastError = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.lastError = truncate(error);
        this.updatedAt = Instant.now();
    }

    public void markPending(String error) {
        this.status = OutboxStatus.PENDING;
        this.lastError = truncate(error);
        this.updatedAt = Instant.now();
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 500 ? error : error.substring(0, 500);
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public String getSandboxHint() {
        return sandboxHint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
