package com.finflow.commerce.catalogue.persistence;

import com.finflow.commerce.catalogue.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * CATEGORY REPOSITORY - Database access for merchant-owned categories.
 *
 * JpaRepository<Category, Long> tells Spring Data that Category is the entity
 * type and Long is its primary-key type. Spring generates the implementation
 * at runtime; method names below are parsed into queries by convention.
 *
 * Persistence layer interface called by application services. Spring Data discovers
 * it below the application root and supplies a JPA proxy; @Repository is not required
 * on this interface (it is useful on a hand-written DAO for exception translation).
 * Removing JpaRepository inheritance would remove that generated CRUD contract.
 * Derived queries use Java property names, not SQL columns; custom JPQL must do the
 * same. Repositories do not authenticate callers: application services must supply
 * owner/seller constraints and define transactions spanning multiple repositories.
 * Native SQL is an alternative for database-specific operations, at a portability cost.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Derived query: Spring parses the method name into a WHERE organisationId
     * = ? ORDER BY name ASC query. Scoping by organisation prevents one
     * merchant from seeing another merchant's categories.
     *
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
     * Optional expresses that the lookup may legitimately find no row. It
     * avoids returning null and forces callers to handle the missing case.
     * IgnoreCase makes the slug comparison case-insensitive.
     *
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
     * An exists query asks the database for existence rather than loading the
     * full Category entity. The database unique constraint remains the final
     * protection against concurrent duplicate creation attempts.
     *
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
