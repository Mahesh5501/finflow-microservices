
package com.finflow.commerce.dto.catalogue;

import com.finflow.commerce.entity.catalogue.Category;

/**
 * Response DTO containing category details.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 */
public record CategoryResponse(
        Long categoryId, // Database-generated category identifier.
        Long organisationId, // Organisation that owns the category.
        String name, // Human-readable category name.
        String slug // URL-friendly category identifier.
) {

    /**
     * Converts a Category entity into a CategoryResponse.
     * Keeping this mapping method here avoids repeating the same entity-to-response conversion in service methods.
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
