
package com.finflow.commerce.entity.cart;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Entity
@Table(
        name = "carts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_user_org",
                columnNames = {"customer_user_id", "organisation_id"}
        )
)
public class Cart {

    // Database-generated primary key.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    // The customer who owns this cart.
    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    // Organisation or tenant to which this cart belongs.
    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    // Updated whenever a cart operation changes its contents.
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // JPA optimistic-locking version.
    // Hibernate uses this to detect conflicting updates.
    @Version
    private Long version;

    // One cart can contain multiple items.
    // Cart owns the lifecycle of its CartItem children.
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    // Required by JPA when loading an existing entity.
    protected Cart() {
    }

    // Application constructor used when creating a new cart.
    public Cart(Long customerUserId, Long organisationId) {
        this.customerUserId = customerUserId;
        this.organisationId = organisationId;
        this.updatedAt = Instant.now();
    }

    // Sets the requested quantity for a product.
    // If quantity is zero or negative, the item is removed.
    public void upsertItem(Long productId, int quantity) {
        if (quantity <= 0) {
            removeItem(productId);
            return;
        }

        // Check whether this product is already in the cart.
        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            // Replace the existing quantity.
            existing.get().setQuantity(quantity);
        } else {
            // Create a new item and connect it to this cart.
            CartItem item = new CartItem(productId, quantity);
            item.setCart(this);
            items.add(item);
        }

        touch();
    }

    // Increases the existing quantity instead of replacing it.
    public void addQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            // Example: existing quantity 2 + requested quantity 3 = 5.
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            CartItem item = new CartItem(productId, quantity);
            item.setCart(this);
            items.add(item);
        }

        touch();
    }

    // Removes the product from the cart.
    public void removeItem(Long productId) {
        items.removeIf(i -> i.getProductId().equals(productId));
        touch();
    }

    // Removes every item from the cart.
    public void clearItems() {
        items.clear();
        touch();
    }

    // Updates the cart timestamp after a cart operation.
    private void touch() {
        this.updatedAt = Instant.now();
    }
}
