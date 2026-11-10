package com.finflow.commerce.integration.event;

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
 * A record is suitable because this event carries data and does not need mutable fields.
 * Contains a snapshot of important order information at the time the event was created.
 * This is also the structure serialized when publishing to Kafka.
 */
public record CommerceDomainEvent(
        CommerceEventType type, // Identifies what business event occurred. Example: ORDER_PAID or ORDER_CANCELLED.
        Long orderId, // Database ID of the order associated with the event. Consumers can use it to correlate related events.
        String orderNumber, // Human-readable order reference. Example: ORD-A1B2C3D4.
        Long organisationId, // Identifies the organisation that owns the order. Important when the application supports multiple organisations.
        Long customerUserId, // Identifies the customer who placed the order.
        BigDecimal totalAmount, // Order total at the time the event was created. BigDecimal is used for monetary precision.
        String currencyCode, // Currency of the order. Example: INR or USD.
        Instant occurredAt // Time at which this event was created. Instant represents a point in time in UTC.
) {
}