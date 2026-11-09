
package com.finflow.commerce.entity;

/**
 * Processing states of a payment outbox record.
 */
public enum OutboxStatus {

    // The payment work has been saved but has not started.
    PENDING,

    // A worker has started processing the payment.
    PROCESSING,

    // Payment processing has completed successfully.
    DONE,

    // Payment processing was rejected or permanently failed.
    FAILED
}
