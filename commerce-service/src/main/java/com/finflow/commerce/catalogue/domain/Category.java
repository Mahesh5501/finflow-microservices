package com.finflow.commerce.catalogue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/**
 * CATEGORY - Catalogue domain entity and JPA persistence model.
 *
 * A category groups products for a merchant organisation. The organisation ownership is part of the category identity in business terms:
 * two merchants may use the same slug, but one merchant cannot create the same slug twice.
 *
 * CONCEPTS USED:
 * - @Entity/@Table: map this class to the relational `categories` table.
 * - Unique constraint: database-level protection against duplicate slugs within the same organisation, even if two requests race concurrently.
 * - Encapsulation: the constructor is the intended creation path; JPA uses the protected no-argument constructor when loading existing rows.
 */
// Lombok generates getters without exposing public setters.
// @Entity registers this type with JPA;
// @Table maps it to the categories table.
@Getter
@Entity
@Table(name = "categories",
        // The pair (organisation_id, slug), rather than slug alone, is unique. This allows different merchants to use e.g. "electronics".
        uniqueConstraints = @UniqueConstraint(
                name = "uk_category_org_slug",
                columnNames = {"organisation_id", "slug"}
        )
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId; // Database-generated category ID.

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;  // Organisation that owns this category.

    @Column(nullable = false, length = 120)
    private String name; // Display name shown to customers.

    @Column(nullable = false, length = 140)
    private String slug; // URL-friendly identifier used to identify the category.

    // JPA requires a no-argument constructor to instantiate entities while reading database rows. PROTECTED keeps it out of normal callers.
    protected Category() {
    }

    /**
     * Creates a category and normalises its name and slug.
     *
     * Normalisation makes category lookup predictable (for example, "Mobile Phones" becomes the slug "mobile-phones" when the caller supplies that slug).
     * API-level validation still belongs in the request DTO; entity-level invariants can be strengthened separately if needed.
     */
    public Category(Long organisationId, String name, String slug) {
        this.organisationId = organisationId;
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
    }
}
