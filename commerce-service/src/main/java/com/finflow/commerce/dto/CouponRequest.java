
package com.finflow.commerce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request DTO used to create a coupon.
 *
 * It contains the organisation, coupon code, discount percentage
 * and optional expiry date.
 */
public record CouponRequest(

        // Organisation that owns this coupon.
        // Required because coupons are organisation-specific.
        @NotNull Long organisationId,

        // Coupon code entered by the customer during checkout.
        // Cannot be null, empty or whitespace-only.
        // Maximum allowed length is 40 characters.
        @NotBlank @Size(max = 40) String code,

        // Percentage discount offered by this coupon.
        // Must be between 1% and 90%, inclusive.
        @NotNull
        @DecimalMin("1.00")
        @DecimalMax("90.00")
        BigDecimal percentOff,

        // Optional expiry date/time.
        // Null means no expiry time is supplied.
        Instant expiresAt

) {
}
