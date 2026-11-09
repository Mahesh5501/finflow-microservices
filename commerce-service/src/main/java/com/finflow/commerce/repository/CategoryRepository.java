
package com.finflow.commerce.repository;

import com.finflow.commerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category database operations.
 * JpaRepository provides common CRUD and pagination-related database operations without requiring manual implementation.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Retrieves all categories belonging to an organisation.
     * Results are sorted alphabetically by category name. Example: Electronics Fashion Furniture
     * @param organisationId the ID of the organisation.
     * @return the categories belonging to the organisation.
     * The organisation condition prevents categories from different organisations from appearing in the same result.
     */
    List<Category> findByOrganisationIdOrderByNameAsc(
            Long organisationId
    );

    /**
     * Finds one category using its organisation ID and slug.
     * IgnoreCase allows searches without worrying about uppercase or lowercase differences. Example: organisationId = 10 slug = "mobile-phones" Optional is empty if the category does not exist.
     * @param organisationId the ID of the organisation.
     * @param slug the slug of the category.
     * @return the category with the given organisation ID and slug.
     */
    Optional<Category> findByOrganisationIdAndSlugIgnoreCase(
            Long organisationId,
            String slug
    );

    /**
     * Checks whether a category with the given slug already exists inside the specified organisation.
     * Returns: true -> matching category exists. false -> matching category does not exist. Used before creating a category to detect duplicate slugs.
     * @param organisationId the ID of the organisation.
     * @param slug the slug of the category.
     * @return true if a category with the given slug already exists inside the specified organisation, false otherwise.
     */
    boolean existsByOrganisationIdAndSlugIgnoreCase(
            Long organisationId,
            String slug
    );
}
