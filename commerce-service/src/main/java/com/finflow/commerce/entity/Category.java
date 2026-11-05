package com.finflow.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_category_org_slug",
                columnNames = {"organisation_id", "slug"}
        )
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 140)
    private String slug;

    protected Category() {
    }

    public Category(Long organisationId, String name, String slug) {
        this.organisationId = organisationId;
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getOrganisationId() {
        return organisationId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }
}
