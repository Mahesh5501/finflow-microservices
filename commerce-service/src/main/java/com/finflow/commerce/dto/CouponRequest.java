package com.finflow.commerce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
        @NotNull Long organisationId,
        @NotBlank @Size(max = 40) String code,
        @NotNull @DecimalMin("1.00") @DecimalMax("90.00") BigDecimal percentOff,
        Instant expiresAt
) {
}
