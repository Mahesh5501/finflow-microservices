package com.finflow.commerce.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request used to create a product category.
 * Seller ownership is resolved from the merchant's JWT — not from this body.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 140) String slug
) {
}
