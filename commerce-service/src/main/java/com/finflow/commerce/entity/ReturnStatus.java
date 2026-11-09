
package com.finflow.commerce.entity;

/**
 * Represents the lifecycle states of a return request.
 *
 * These values are persisted in the database as strings
 * through @Enumerated(EnumType.STRING) in ReturnRequest.
 */
public enum ReturnStatus {

    /**
     * Customer has submitted a return request.
     * The organisation has not yet approved or rejected it.
     */
    REQUESTED,

    /**
     * Organisation has approved the return.
     * The return can now proceed towards completion.
     */
    APPROVED,

    /**
     * Organisation has rejected the return.
     * This is a terminal state in the current implementation.
     */
    REJECTED,

    /**
     * Return process has been completed.
     * Refund and restocking operations have been finalized.
     */
    COMPLETED
}
