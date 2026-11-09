
package com.finflow.commerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request used to create a product in the catalogue.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 * Contains the organisation, product name, price, available stock and optional category association.
 */
public record ProductRequest(
        @NotNull Long organisationId, // Organisation that owns the product.
        @NotBlank String name, // Product name. Must not be null, empty or whitespace-only.
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice, // Unit selling price. Must be provided and greater than or equal to 0.01.
        @Min(0) int availableQuantity, // Initial available stock quantity. Zero is allowed, but negative stock is not.
        Long categoryId // Optional category to which this product belongs. Null means the product is not assigned to a category.
) {
}
