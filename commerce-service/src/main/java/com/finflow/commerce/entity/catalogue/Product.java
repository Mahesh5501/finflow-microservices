
package com.finflow.commerce.entity.catalogue;

import com.finflow.commerce.exception.ApiException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public Product(
            Long organisationId,
            String name,
            BigDecimal unitPrice,
            int availableQuantity,
            Long categoryId
    ) {
        if (organisationId == null || organisationId <= 0) {
            throw ApiException.badRequest("Organisation ID must be positive");
        }

        if (name == null || name.isBlank() || name.trim().length() > 200) {
            throw ApiException.badRequest("Product name must contain 1 to 200 characters");
        }

        validatePrice(unitPrice);

        if (availableQuantity < 0) {
            throw ApiException.badRequest("Available quantity cannot be negative");
        }

        if (categoryId != null && categoryId <= 0) {
            throw ApiException.badRequest("Category ID must be positive");
        }

        this.organisationId = organisationId;
        this.name = name.trim();
        this.unitPrice = unitPrice;
        this.availableQuantity = availableQuantity;
        this.categoryId = categoryId;
        this.status = ProductStatus.ACTIVE;
    }

    public void assignCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw ApiException.badRequest("Category ID must be positive");
        }

        this.categoryId = categoryId;
    }

    public void reserveStock(int quantity) {
        if (quantity <= 0) {
            throw ApiException.badRequest("Reservation quantity must be positive");
        }

        if (status != ProductStatus.ACTIVE) {
            throw ApiException.conflict(
                    "PRODUCT_INACTIVE",
                    "Cannot reserve stock for an inactive product"
            );
        }

        if (availableQuantity < quantity) {
            throw ApiException.conflict(
                    "INSUFFICIENT_STOCK",
                    "Insufficient stock available"
            );
        }

        this.availableQuantity -= quantity;
    }

    public void releaseStock(int quantity) {
        if (quantity <= 0) {
            throw ApiException.badRequest("Release quantity must be positive");
        }

        try {
            this.availableQuantity = Math.addExact(this.availableQuantity, quantity);
        } catch (ArithmeticException ex) {
            throw ApiException.conflict(
                    "STOCK_OVERFLOW",
                    "Available stock exceeds the supported quantity"
            );
        }
    }

    public void updateUnitPrice(BigDecimal unitPrice) {
        validatePrice(unitPrice);
        this.unitPrice = unitPrice;
    }

    public void updateAvailableQuantity(int availableQuantity) {
        if (availableQuantity < 0) {
            throw ApiException.badRequest("Available quantity cannot be negative");
        }

        this.availableQuantity = availableQuantity;
    }

    private void validatePrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw ApiException.badRequest("Unit price must be positive");
        }
    }
}
