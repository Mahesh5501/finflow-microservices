package com.finflow.commerce.catalogue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request used to create a product in the catalogue.
 * Seller ownership is resolved from the merchant's JWT via {@link com.finflow.commerce.seller.MerchantSellerAccess}.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record ProductRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        @Min(0) int availableQuantity,
        Long categoryId // Optional category within the merchant's catalogue; validated in CatalogueService.
) {
}
