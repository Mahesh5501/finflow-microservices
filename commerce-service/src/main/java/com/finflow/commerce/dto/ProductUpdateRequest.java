package com.finflow.commerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/** Partial update — null fields are left unchanged. */
public record ProductUpdateRequest(
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal unitPrice,
        @Min(0) Integer availableQuantity,
        Long categoryId
) {
}
