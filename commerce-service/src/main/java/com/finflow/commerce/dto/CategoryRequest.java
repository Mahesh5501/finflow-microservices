
package com.finflow.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request used to create a product category.
 * The organisation ID establishes which tenant owns the category.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record CategoryRequest(
        @NotNull Long organisationId, // Organisation in which the category will be created.
        @NotBlank @Size(max = 120) String name, // Human-readable category name. Cannot be null, empty or whitespace-only. Maximum allowed length is 120 characters.
        @NotBlank @Size(max = 140) String slug // URL-friendly category identifier. Cannot be blank and has a maximum length of 140.
) {
}
