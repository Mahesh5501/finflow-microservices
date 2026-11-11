package com.finflow.commerce.catalogue.domain;

import com.finflow.commerce.exception.ApiException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * PRODUCT - Catalogue domain entity and JPA persistence model.
 *
 * BUSINESS RESPONSIBILITY:
 * Represents a product owned by one merchant organisation. It keeps the
 * product's catalogue data and stock state together so business rules such as
 * stock reservation are enforced in one place instead of being scattered
 * across controllers or repositories.
 *
 * CONCEPTS USED:
 * - ORM/JPA: Hibernate maps this Java object to the `products` table.
 * - Encapsulation: fields are private; callers change state through methods
 *   that validate business rules.
 * - Optimistic locking: `@Version` detects concurrent updates at commit time.
 * - BigDecimal: monetary values avoid floating-point rounding errors.
 *
 * IMPORTANT DESIGN CHOICE:
 * This is both a JPA entity and a domain object (a pragmatic approach for this
 * modular monolith). We are not creating a second copy of Product just to
 * separate persistence from domain state.
 */
// @Entity makes this class managed by JPA. @Table explicitly selects
// the database table. Lombok @Getter generates read-only accessors so callers
// can inspect state without getting unrestricted setters.
@Entity
@Table(name = "products")
@Getter
// JPA needs a no-argument constructor to hydrate an entity from a row.
// PROTECTED prevents ordinary application code from creating an invalid,
// partially initialised Product with `new Product()`.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    // @Id marks the primary key; @GeneratedValue delegates ID creation to
    // the database identity/auto-increment mechanism.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    // Every product belongs to a merchant organisation. nullable=false
    // adds a database NOT NULL constraint as a second line of defence.
    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    // Nullable because a merchant may create a product before assigning
    // it to a category. This stores the category ID without a JPA relationship.
    @Column(name = "category_id")
    private Long categoryId;

    // Length and nullability mirror the database column constraints.
    @Column(nullable = false, length = 200)
    private String name;

    // BigDecimal is used for money; precision=19 and scale=4 define the
    // database numeric capacity and number of digits after the decimal point.
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    // STRING persists values such as ACTIVE, not ordinal numbers (0, 1).
    // Reordering enum constants therefore will not corrupt existing rows.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    // Optimistic-lock version. Hibernate includes the previous version in
    // UPDATE statements and increments it on success. If another transaction
    // updated this row first, the update affects zero rows and a concurrency
    // conflict is raised instead of silently overwriting newer stock.
    @Version
    private Long version;

    /**
     * Creates a valid new product. Validation lives here as well as at the API
     * boundary so non-HTTP callers cannot bypass core product invariants.
     * A newly created product starts ACTIVE; stock starts at the supplied value.
     */
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

    /** Assigns a valid category to this product. */
    public void assignCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw ApiException.badRequest("Category ID must be positive");
        }

        this.categoryId = categoryId;
    }

    /**
     * Reserves stock for an order.
     *
     * The method validates the requested quantity, product state, and current
     * availability before changing the in-memory entity. The caller must save
     * the entity within a transaction so the database update and @Version
     * check happen atomically.
     */
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

    /**
     * Returns previously reserved stock when an order is cancelled or a
     * reservation is otherwise released. Math.addExact protects against
     * integer overflow, which could otherwise produce a negative stock value.
     */
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

    /** Changes the selling price after applying the same positive-price rule. */
    public void updateUnitPrice(BigDecimal unitPrice) {
        validatePrice(unitPrice);
        this.unitPrice = unitPrice;
    }

    /**
     * Sets stock to an explicitly supplied quantity. This is an administrative
     * stock adjustment; order reservation should use reserveStock instead.
     */
    public void updateAvailableQuantity(int availableQuantity) {
        if (availableQuantity < 0) {
            throw ApiException.badRequest("Available quantity cannot be negative");
        }

        this.availableQuantity = availableQuantity;
    }

    // Keep price validation inside the entity so every caller (not only
    // the REST API) must obey the same positive-price business invariant.
    private void validatePrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw ApiException.badRequest("Unit price must be positive");
        }
    }
}
