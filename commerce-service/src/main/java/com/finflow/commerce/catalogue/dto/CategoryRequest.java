package com.finflow.commerce.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request used to create a product category.
 * The organisation ID establishes which tenant owns the category.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record CategoryRequest(
        @NotNull Long organisationId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 140) String slug
) {
}
