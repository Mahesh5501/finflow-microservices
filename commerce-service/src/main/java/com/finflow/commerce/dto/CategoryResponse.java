
package com.finflow.commerce.dto;

import com.finflow.commerce.entity.Category;

/**
 * Response DTO containing category details.
 *
 * The static from() method converts a JPA Category entity
 * into the API response representation.
 */
public record CategoryResponse(

        // Database-generated category identifier.
        Long categoryId,

        // Organisation that owns the category.
        Long organisationId,

        // Human-readable category name.
        String name,

        // URL-friendly category identifier.
        String slug

) {

    /**
     * Converts a Category entity into a CategoryResponse.
     *
     * Keeping this mapping method here avoids repeating the
     * same entity-to-response conversion in service methods.
     */
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getOrganisationId(),
                category.getName(),
                category.getSlug()
        );
    }
}
