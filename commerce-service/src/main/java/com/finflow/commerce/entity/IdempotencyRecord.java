package com.finflow.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, Long organisationId, Long orderId) {
        this.idempotencyKey = idempotencyKey;
        this.organisationId = organisationId;
        this.orderId = orderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getOrderId() {
        return orderId;
    }
}
