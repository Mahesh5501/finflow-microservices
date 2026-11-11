package com.finflow.commerce.order.domain;

/**
 * Processing states of a payment outbox record.
 */
public enum OutboxStatus {

    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
