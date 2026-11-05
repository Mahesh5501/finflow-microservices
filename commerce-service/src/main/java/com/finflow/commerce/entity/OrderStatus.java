package com.finflow.commerce.entity;

/**
 * Distinct payment / cancel outcomes — never collapse into a single FAILED.
 * {@code EXPIRED} = reservation TTL elapsed without successful payment.
 */
public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    CONFIRMED,
    PAYMENT_FAILED,
    ACTION_REQUIRED,
    CANCELLED,
    EXPIRED,
    FULFILMENT_PENDING,
    FULFILLED
}
