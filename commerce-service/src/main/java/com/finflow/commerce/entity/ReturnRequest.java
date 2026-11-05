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

@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReturnRequest() {
    }

    public ReturnRequest(Long orderId, Long customerUserId, String reason) {
        this.orderId = orderId;
        this.customerUserId = customerUserId;
        this.reason = reason;
        this.status = ReturnStatus.REQUESTED;
        this.createdAt = Instant.now();
    }

    public void approve() {
        if (status != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED returns can be approved");
        }
        this.status = ReturnStatus.APPROVED;
    }

    public void complete() {
        if (status != ReturnStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED returns can be completed");
        }
        this.status = ReturnStatus.COMPLETED;
    }

    public void reject() {
        if (status != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED returns can be rejected");
        }
        this.status = ReturnStatus.REJECTED;
    }

    public Long getReturnId() {
        return returnId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getCustomerUserId() {
        return customerUserId;
    }

    public String getReason() {
        return reason;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
