package com.finflow.commerce.integration.event;

/**
 * Defines the business event types that Commerce Service can publish.
 * Using an enum restricts the event type to a known set of values.
 * These names are also important to consumers because they identify which business operation occurred.
 */
public enum CommerceEventType {
    ORDER_PAID, // The customer's payment succeeded and the order was paid.
    ORDER_CANCELLED, // An existing order was cancelled.
    ORDER_EXPIRED, // A pending order expired because its reservation was not completed.
    ORDER_FULFILLED, // The order was marked as fulfilled.
    RETURN_COMPLETED // The return process for an order was completed.
}