package com.finflow.notification.integration.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Wire contract from topic {@code commerce.events} (owned by commerce-service). */
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
