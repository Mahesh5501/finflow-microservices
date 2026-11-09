package com.finflow.commerce.event;

/**
 * Defines the business event types that Commerce Service can publish.
 *
 * Using an enum restricts the event type to a known set of values.
 *
 * These names are also important to consumers because they identify
 * which business operation occurred.
 */
public enum CommerceEventType {

    // The customer's payment succeeded and the order was paid.
    ORDER_PAID,

    // An existing order was cancelled.
    ORDER_CANCELLED,

    // A pending order expired because its reservation was not completed.
    ORDER_EXPIRED,

    // The order was marked as fulfilled.
    ORDER_FULFILLED,

    // The return process for an order was completed.
    RETURN_COMPLETED
}