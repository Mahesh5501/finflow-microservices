package com.finflow.commerce.entity.returns;

/**
 * Lifecycle states of a return request (persisted as STRING).
 */
public enum ReturnStatus {

    REQUESTED,
    APPROVED,
    REJECTED,
    COMPLETED
}
