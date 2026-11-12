package com.finflow.commerce.coupon.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request used to create a coupon.
 * Seller ownership is resolved from the merchant's JWT — coupons remain seller-scoped in persistence.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record CouponRequest(
        @NotBlank @Size(max = 40) String code, // Coupon code entered at checkout; max 40 characters.
        @NotNull @DecimalMin("1.00") @DecimalMax("90.00") BigDecimal percentOff, // Between 1% and 90% inclusive.
        Instant expiresAt // Optional expiry; null means no expiry time on the request.
) {
}
