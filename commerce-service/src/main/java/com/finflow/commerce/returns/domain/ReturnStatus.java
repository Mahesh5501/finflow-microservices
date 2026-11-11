package com.finflow.commerce.returns.domain;

/**
 * Lifecycle states of a return request (persisted as STRING).
 */
public enum ReturnStatus {

    REQUESTED,
    APPROVED,
    REJECTED,
    COMPLETED
}
