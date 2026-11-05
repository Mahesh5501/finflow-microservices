package com.finflow.commerce.dto;

import com.finflow.commerce.entity.Category;

public record CategoryResponse(
        Long categoryId,
        Long organisationId,
        String name,
        String slug
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getOrganisationId(),
                category.getName(),
                category.getSlug()
        );
    }
}
