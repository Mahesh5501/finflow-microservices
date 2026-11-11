package com.finflow.commerce.coupon.persistence;

import com.finflow.commerce.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for performing database operations on Coupon entities.
 * Spring Data JPA automatically provides the implementation. We do not need to write an implementation class or SQL query for the methods supported by Spring Data's derived-query mechanism.
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
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Finds a coupon using its organisation ID and coupon code.
     * Organisation ID ensures that one organisation cannot accidentally use another organisation's coupon. IgnoreCase makes the coupon-code comparison case-insensitive. Example: organisationId = 10, code = "WELCOME10" This can also match "welcome10". Optional: - Contains a Coupon when a matching record exists. - Is empty when no matching coupon exists.
     * @param organisationId the ID of the organisation.
     * @param code the coupon code.
     * @return the coupon with the given organisation ID and coupon code.
     * Optional is empty when no matching coupon exists.
     */
    Optional<Coupon> findByOrganisationIdAndCodeIgnoreCase(
            Long organisationId,
            String code
    );
}
