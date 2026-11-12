package com.finflow.notification.integration.event;

/** Must stay aligned with commerce-service {@code CommerceEventType}. */
public enum CommerceEventType {
    ORDER_PAID,
    ORDER_CANCELLED,
    ORDER_EXPIRED,
    ORDER_FULFILLED,
    RETURN_COMPLETED
}
