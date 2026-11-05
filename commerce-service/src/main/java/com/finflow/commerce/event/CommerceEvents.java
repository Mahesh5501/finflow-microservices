package com.finflow.commerce.event;

import com.finflow.commerce.entity.CommerceOrder;

import java.time.Instant;

public final class CommerceEvents {

    private CommerceEvents() {
    }

    public static CommerceDomainEvent of(CommerceOrder order, CommerceEventType type) {
        return new CommerceDomainEvent(
                type,
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrganisationId(),
                order.getCustomerUserId(),
                order.getTotalAmount(),
                order.getCurrencyCode(),
                Instant.now()
        );
    }
}
