
package com.finflow.commerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request used to create a product in the catalogue.
 *
 * Contains the organisation, product name, price, available stock
 * and optional category association.
 */
public record ProductRequest(

        // Organisation that owns the product.
        @NotNull Long organisationId,

        // Product name.
        // Must not be null, empty or whitespace-only.
        @NotBlank String name,

        // Unit selling price.
        // Must be provided and greater than or equal to 0.01.
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,

        // Initial available stock quantity.
        // Zero is allowed, but negative stock is not.
        @Min(0) int availableQuantity,

        // Optional category to which this product belongs.
        // Null means the product is not assigned to a category.
        Long categoryId

) {
}
