package com.finflow.commerce.repository.cart;

import com.finflow.commerce.entity.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Database access for customer carts.
 * A cart is identified by the customer and organisation combination.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Finds the cart belonging to a particular customer within a particular organisation.
     * Optional.empty() means the customer does not have a cart yet.
     * @param customerUserId the ID of the customer.
     * @param organisationId the ID of the organisation.
     * @return the cart belonging to the customer and organisation.
     */
    Optional<Cart> findByCustomerUserIdAndOrganisationId(
            Long customerUserId,
            Long organisationId
    );

    List<Cart> findByCustomerUserId(Long customerUserId);

    /**
     * Finds carts that have not been updated since the given instant.
     * Used by AbandonedCartCleanupService.
     * @param updatedBefore the instant before which the carts must have been updated.
     * @return the carts that have not been updated since the given instant.
     */
    List<Cart> findByUpdatedAtBefore(Instant updatedBefore);
}