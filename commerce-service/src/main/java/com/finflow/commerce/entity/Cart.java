package com.finflow.commerce.entity;

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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(
        name = "carts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_user_org",
                columnNames = {"customer_user_id", "organisation_id"}
        )
)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(Long customerUserId, Long organisationId) {
        this.customerUserId = customerUserId;
        this.organisationId = organisationId;
        this.updatedAt = Instant.now();
    }

    public void upsertItem(Long productId, int quantity) {
        if (quantity <= 0) {
            removeItem(productId);
            return;
        }
        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setQuantity(quantity);
        } else {
            CartItem item = new CartItem(productId, quantity);
            item.setCart(this);
            items.add(item);
        }
        touch();
    }

    public void addQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            CartItem item = new CartItem(productId, quantity);
            item.setCart(this);
            items.add(item);
        }
        touch();
    }

    public void removeItem(Long productId) {
        items.removeIf(i -> i.getProductId().equals(productId));
        touch();
    }

    public void clearItems() {
        items.clear();
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getCartId() {
        return cartId;
    }

    public Long getCustomerUserId() {
        return customerUserId;
    }

    public Long getOrganisationId() {
        return organisationId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<CartItem> getItems() {
        return items;
    }
}
