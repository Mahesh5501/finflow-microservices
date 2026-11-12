package com.finflow.commerce.catalogue.dto;

import com.finflow.commerce.catalogue.domain.Category;
import java.io.Serializable;

/**
 * Response DTO containing category details.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 */
public record CategoryResponse(Long categoryId, Long sellerId, String name, String slug) implements Serializable {

    // Boot's default Redis cache serializer uses Java serialization. The immutable DTO is safe to cache; persisting a JPA entity would also capture proxy state.
    private static final long serialVersionUID = 1L;

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
