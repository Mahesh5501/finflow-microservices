package com.finflow.commerce.order.application;

import com.finflow.commerce.order.domain.CommerceOrder;
import com.finflow.commerce.integration.event.CommerceDomainEvent;
import com.finflow.commerce.integration.event.CommerceEventType;

import java.time.Instant;

/**
 * Utility/factory class for creating CommerceDomainEvent instances.
 * It converts a CommerceOrder entity into the event data contract.
 * This avoids duplicating the same field-mapping code in:
 * - PaymentSettlementService
 * - OrderCancelService
 * - ReservationExpiryService
 * - CommerceService
 * - ReturnService
 *
 * Order application mapping helper shared with ReturnService. It maps a
 * CommerceOrder snapshot to the transport-neutral CommerceDomainEvent contract.
 * Keeping this mapper here makes integration.event a leaf package with no dependency
 * back into order domain; event publishers know event data, not JPA aggregates.
 * It is a stateless utility, so no Spring bean or injected mapper interface is needed.
 * The caller chooses publication timing; constructing an event does not persist it.
 */
public final class CommerceEvents {

    /**
     * Private constructor prevents this utility class from being instantiated.
     * All methods in this class are static, so an object is unnecessary.
     */
    private CommerceEvents() {
    }

    /**
     * Creates an event from an order and the event type.
     * @param order the order associated with the event
     * @param type the business event that occurred
     * @return an immutable event containing a snapshot of order details
     */
    public static CommerceDomainEvent of(CommerceOrder order, // The order associated with the event
            CommerceEventType type // Identifies what business event occurred. Example: ORDER_PAID or ORDER_CANCELLED.
    ) {
        return new CommerceDomainEvent(
                type, // Identifies what business event occurred. Example: ORDER_PAID or ORDER_CANCELLED.
                order.getOrderId(), // Database ID of the order associated with the event. Consumers can use it to correlate related events.
                order.getOrderNumber(), // Human-readable order reference. Example: ORD-A1B2C3D4.
                order.getOrganisationId(), // Identifies the organisation that owns the order. Important when the application supports multiple organisations.
                order.getCustomerUserId(), // Identifies the customer who placed the order.
                order.getTotalAmount(), // Order total at the time the event was created. BigDecimal is used for monetary precision.
                order.getCurrencyCode(), // Currency of the order. Example: INR or USD.
                Instant.now() // Time at which this event was created. Instant represents a point in time in UTC.
        );
    }
}
