package com.finflow.commerce.entity.order;

/**
 * Processing states of a payment outbox record.
 */
public enum OutboxStatus {

    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
