package com.finflow.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Version
    private Long version;

    protected Product() {
    }

    public Product(Long organisationId, String name, BigDecimal unitPrice, int availableQuantity, Long categoryId) {
        this.organisationId = organisationId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.availableQuantity = availableQuantity;
        this.categoryId = categoryId;
        this.status = ProductStatus.ACTIVE;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getOrganisationId() {
        return organisationId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public void assignCategory(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void releaseStock(int qty) {
        this.availableQuantity += qty;
    }

    public void reserveStock(int qty) {
        if (qty <= 0 || availableQuantity < qty) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.availableQuantity -= qty;
    }

    public void updateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
        this.unitPrice = unitPrice;
    }

    public void updateAvailableQuantity(int availableQuantity) {
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Available quantity cannot be negative");
        }
        this.availableQuantity = availableQuantity;
    }
}
