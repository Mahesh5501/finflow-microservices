package com.finflow.commerce.repository;

import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Database access for CommerceOrder.
 *
 * Spring Data JPA generates the implementation at runtime.
 */
public interface CommerceOrderRepository
        extends JpaRepository<CommerceOrder, Long> {

    /**
     * Finds orders belonging to one customer within one organisation.
     *
     * Results are ordered newest first.
     */
    List<CommerceOrder>
    findByOrganisationIdAndCustomerUserIdOrderByCreatedAtDesc(
            Long organisationId,
            Long customerUserId
    );

    /**
     * Finds all orders belonging to one organisation.
     *
     * Results are ordered newest first.
     */
    List<CommerceOrder> findByOrganisationIdOrderByCreatedAtDesc(
            Long organisationId
    );

    /**
     * Finds orders whose status is in the supplied collection
     * and whose creation time is earlier than the supplied instant.
     *
     * Used by ReservationExpiryService to find stale unpaid orders.
     */
    List<CommerceOrder> findByStatusInAndCreatedAtBefore(
            Collection<OrderStatus> statuses,
            Instant createdBefore
    );
}