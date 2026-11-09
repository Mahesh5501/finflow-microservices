package com.finflow.commerce.event;

import com.finflow.commerce.entity.CommerceOrder;

import java.time.Instant;

/**
 * Utility/factory class for creating CommerceDomainEvent instances.
 *
 * It converts a CommerceOrder entity into the event data contract.
 *
 * This avoids duplicating the same field-mapping code in:
 * - PaymentSettlementService
 * - OrderCancelService
 * - ReservationExpiryService
 * - CommerceService
 * - ReturnService
 */
public final class CommerceEvents {

    /**
     * Private constructor prevents this utility class from being instantiated.
     *
     * All methods in this class are static, so an object is unnecessary.
     */
    private CommerceEvents() {
    }

    /**
     * Creates an event from an order and the event type.
     *
     * @param order the order associated with the event
     * @param type the business event that occurred
     * @return an immutable event containing a snapshot of order details
     */
    public static CommerceDomainEvent of(
            CommerceOrder order,
            CommerceEventType type
    ) {

        // Copy only the required order fields into the event.
        //
        // Instant.now() records when this event object is created.
        // It is not necessarily the exact time the business transaction
        // committed in the database.
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