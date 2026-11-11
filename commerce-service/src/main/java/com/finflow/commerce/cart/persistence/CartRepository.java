package com.finflow.commerce.cart.persistence;

import com.finflow.commerce.cart.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Database access for customer carts.
 * A cart is identified by the customer and organisation combination.
 *
 * Persistence layer interface called by application services. Spring Data discovers
 * it below the application root and supplies a JPA proxy; @Repository is not required
 * on this interface (it is useful on a hand-written DAO for exception translation).
 * Removing JpaRepository inheritance would remove that generated CRUD contract.
 * Derived queries use Java property names, not SQL columns; custom JPQL must do the
 * same. Repositories do not authenticate callers: application services must supply
 * owner/seller constraints and define transactions spanning multiple repositories.
 * Native SQL is an alternative for database-specific operations, at a portability cost.
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