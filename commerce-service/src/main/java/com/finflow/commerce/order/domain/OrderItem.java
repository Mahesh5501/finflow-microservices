package com.finflow.commerce.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Order domain child created by OrderPlacementService through CommerceOrder.
 * OrderResponse mapping reads this historical name/price/quantity rather than current
 * catalogue values. Dependencies are its parent and JPA; productId is a scalar reference.
 * @Entity/@Table keep order_items, @Id/IDENTITY use the existing generated key, and
 * @ManyToOne/@JoinColumn map the required order foreign key. Removing the association
 * breaks the aggregate mapping; a scalar order ID would need explicit persistence
 * coordination. LAZY avoids fetching the parent for every line. The constructor checks
 * identity, name, price and positive quantity; lineTotal uses BigDecimal exactly.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @Getter(AccessLevel.NONE)
    private CommerceOrder order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name_snapshot", nullable = false, length = 200)
    private String productNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false)
    private int quantity;

    public OrderItem(
            Long productId,
            String productNameSnapshot,
            BigDecimal unitPriceSnapshot,
            int quantity
    ) {
        validate(productId, productNameSnapshot, unitPriceSnapshot, quantity);

        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot.trim();
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
    }

    private void validate(
            Long productId,
            String productNameSnapshot,
            BigDecimal unitPriceSnapshot,
            int quantity
    ) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }

        if (productNameSnapshot == null || productNameSnapshot.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }

        if (productNameSnapshot.trim().length() > 200) {
            throw new IllegalArgumentException("Product name cannot exceed 200 characters");
        }

        if (unitPriceSnapshot == null || unitPriceSnapshot.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    void setOrder(CommerceOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        this.order = order;
    }

    public BigDecimal lineTotal() {
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }
}
