package com.finflow.commerce.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Stable wire contract for Kafka (and in-process logging publisher). */
public record CommerceDomainEvent(
        CommerceEventType type,
        Long orderId,
        String orderNumber,
        Long organisationId,
        Long customerUserId,
        BigDecimal totalAmount,
        String currencyCode,
        Instant occurredAt
) {
}
