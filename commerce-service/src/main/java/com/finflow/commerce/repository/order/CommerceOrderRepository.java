package com.finflow.commerce.repository.order;

import com.finflow.commerce.entity.order.CommerceOrder;
import com.finflow.commerce.entity.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Database access for CommerceOrder.
 * Spring Data JPA generates the implementation at runtime.
 */
public interface CommerceOrderRepository
        extends JpaRepository<CommerceOrder, Long> {

    /**
     * Finds orders belonging to one customer within one organisation.
     * Results are ordered newest first.
     * @param organisationId the ID of the organisation.
     * @param customerUserId the ID of the customer.
     * @return the orders belonging to the customer and organisation.
     */
    List<CommerceOrder>
    findByOrganisationIdAndCustomerUserIdOrderByCreatedAtDesc(
            Long organisationId,
            Long customerUserId
    );

    /**
     * Finds all orders belonging to one organisation.
     * Results are ordered newest first.
     * @param organisationId the ID of the organisation.
     * @return the orders belonging to the organisation.
     */
    List<CommerceOrder> findByOrganisationIdOrderByCreatedAtDesc(
            Long organisationId
    );

    /**
     * Finds orders whose status is in the supplied collection and whose creation time is earlier than the supplied instant. Used by ReservationExpiryService to find stale unpaid orders.
     * @param statuses the statuses of the orders.
     * @param createdBefore the instant before which the orders must have been created.
     * @return the orders whose status is in the supplied collection and whose creation time is earlier than the supplied instant.
     */
    List<CommerceOrder> findByStatusInAndCreatedAtBefore(
            Collection<OrderStatus> statuses,
            Instant createdBefore
    );

    List<CommerceOrder> findByCustomerUserIdAndOrganisationIdInOrderByCreatedAtDesc(
            Long customerUserId,
            Collection<Long> organisationIds
    );

    List<CommerceOrder> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId);
}