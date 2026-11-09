
package com.finflow.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_category_org_slug",
                columnNames = {"organisation_id", "slug"}
        )
)
public class Category {

    // Database-generated category ID.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    // Organisation that owns this category.
    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    // Display name shown to customers.
    @Column(nullable = false, length = 120)
    private String name;

    // URL-friendly identifier used to identify the category.
    @Column(nullable = false, length = 140)
    private String slug;

    // Required by JPA.
    protected Category() {
    }

    // Creates a category and normalises its name and slug.
    public Category(Long organisationId, String name, String slug) {
        this.organisationId = organisationId;
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
    }
}
