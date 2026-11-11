package com.finflow.commerce.catalogue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/**
 * Request DTO for partially updating an existing product.
 * A record is suitable because this DTO carries request data and does not need mutable fields. 
 * A null field means that the caller does not want to change that particular value.
 */
public record ProductUpdateRequest(
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal unitPrice,
        @Min(0) Integer availableQuantity,
        Long categoryId
) {
}
