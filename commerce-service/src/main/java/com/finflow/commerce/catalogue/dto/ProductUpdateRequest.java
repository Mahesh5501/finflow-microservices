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
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal unitPrice, // Optional new product price. If null, the existing price remains unchanged. If supplied, it must be at least 0.01.
        @Min(0) Integer availableQuantity, // Optional new available stock quantity. If null, the existing stock remains unchanged. If supplied, it cannot be negative.
        Long categoryId // Optional category ID. If null, the existing category remains unchanged according to the current update-service behaviour.
) {
}
