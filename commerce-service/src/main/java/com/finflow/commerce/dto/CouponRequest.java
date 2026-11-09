
package com.finflow.commerce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request used to create a coupon.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record CouponRequest(
        @NotNull Long organisationId, // Organisation that owns this coupon. Required because coupons are organisation-specific.
        @NotBlank @Size(max = 40) String code, // Coupon code entered by the customer during checkout. Cannot be null, empty or whitespace-only. Maximum allowed length is 40 characters.
        @NotNull @DecimalMin("1.00") @DecimalMax("90.00") BigDecimal percentOff, // Percentage discount offered by this coupon. Must be between 1% and 90%, inclusive.
        Instant expiresAt // Optional expiry date/time. Null means no expiry time is supplied.
) {
}
