
package com.finflow.commerce.dto;

import com.finflow.commerce.entity.Coupon;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO representing a coupon.
 *
 * It exposes coupon details without returning the JPA entity directly.
 */
public record CouponResponse(

        // Database-generated coupon identifier.
        Long couponId,

        // Organisation that owns this coupon.
        Long organisationId,

        // Normalised coupon code.
        String code,

        // Discount percentage.
        BigDecimal percentOff,

        // Whether the coupon is currently active.
        boolean active,

        // Optional coupon expiry date/time.
        Instant expiresAt

) {

    /**
     * Converts a Coupon entity into a CouponResponse.
     *
     * This keeps entity-to-response conversion in one place.
     */
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getCouponId(),
                coupon.getOrganisationId(),
                coupon.getCode(),
                coupon.getPercentOff(),
                coupon.isActive(),
                coupon.getExpiresAt()
        );
    }
}
