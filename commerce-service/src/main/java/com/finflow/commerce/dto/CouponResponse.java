
package com.finflow.commerce.dto;

import com.finflow.commerce.entity.Coupon;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO representing a coupon.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 */
public record CouponResponse(
        Long couponId, // Database-generated coupon identifier.
        Long organisationId, // Organisation that owns this coupon.
        String code, // Normalised coupon code.
        BigDecimal percentOff, // Discount percentage.
        boolean active, // Whether the coupon is currently active.
        Instant expiresAt // Optional coupon expiry date/time.
) {

    /**
     * Converts a Coupon entity into a CouponResponse.
     * Keeping this mapping method here avoids repeating the same entity-to-response conversion in service methods.
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
