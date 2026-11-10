
package com.finflow.commerce.repository.coupon;

import com.finflow.commerce.entity.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for performing database operations on Coupon entities.
 * Spring Data JPA automatically provides the implementation. We do not need to write an implementation class or SQL query for the methods supported by Spring Data's derived-query mechanism.
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
