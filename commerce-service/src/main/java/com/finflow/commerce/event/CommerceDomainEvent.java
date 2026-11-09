package com.finflow.commerce.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents an event produced by Commerce Service.
 *
 * This record is the event data contract shared by:
 * - The logging publisher.
 * - The Kafka publisher.
 * - Any future event consumers that understand this schema.
 *
 * It contains a snapshot of important order information at the time
 * the event was created.
 *
 * This is also the structure serialized when publishing to Kafka.
 */
public record CommerceDomainEvent(

        // Identifies what business event occurred.
        // Example: ORDER_PAID or ORDER_CANCELLED.
        CommerceEventType type,

        // Database ID of the order associated with the event.
        // Consumers can use it to correlate related events.
        Long orderId,

        // Human-readable order reference.
        // Example: ORD-A1B2C3D4.
        String orderNumber,

        // Identifies the organisation that owns the order.
        // Important when the application supports multiple organisations.
        Long organisationId,

        // Identifies the customer who placed the order.
        Long customerUserId,

        // Order total at the time the event was created.
        // BigDecimal is used for monetary precision.
        BigDecimal totalAmount,

        // Currency of the order.
        // Example: INR or USD.
        String currencyCode,

        // Time at which this event was created.
        // Instant represents a point in time in UTC.
        Instant occurredAt

) {
}