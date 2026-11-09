package com.finflow.commerce.repository;

import com.finflow.commerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Database access for customer carts.
 *
 * A cart is identified by the customer and organisation combination.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Finds the cart belonging to a particular customer
     * within a particular organisation.
     *
     * Optional.empty() means the customer does not have a cart yet.
     */
    Optional<Cart> findByCustomerUserIdAndOrganisationId(
            Long customerUserId,
            Long organisationId
    );

    /**
     * Finds carts that have not been updated since the given instant.
     *
     * Used by AbandonedCartCleanupService.
     */
    List<Cart> findByUpdatedAtBefore(Instant updatedBefore);
}