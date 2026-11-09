
package com.finflow.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request used to create a product category.
 *
 * The organisation ID establishes which tenant owns the category.
 */
public record CategoryRequest(

        // Organisation in which the category will be created.
        @NotNull Long organisationId,

        // Human-readable category name.
        // Cannot be null, empty or whitespace-only.
        // Maximum allowed length is 120 characters.
        @NotBlank @Size(max = 120) String name,

        // URL-friendly category identifier.
        // Cannot be blank and has a maximum length of 140.
        @NotBlank @Size(max = 140) String slug

) {
}
