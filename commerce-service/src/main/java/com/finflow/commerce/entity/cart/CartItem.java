
package com.finflow.commerce.entity.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_item_product",
                columnNames = {"cart_id", "product_id"}
        )
)
public class CartItem {

    // Primary key of this cart item.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long cartItemId;

    // Many cart items can belong to one cart.
    // LAZY means the cart is not automatically fetched every time
    // a CartItem is loaded.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // Product identifier stored in the cart.
    // This is an ID field, not a JPA relationship to Product.
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Number of units of this product.
    @Column(nullable = false)
    private int quantity;

    // Required by JPA.
    protected CartItem() {
    }

    // Creates a cart item for a product.
    public CartItem(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // Package-private: Cart uses this to maintain the relationship.
    // Keeping it non-public limits changes from unrelated packages.
    void setCart(Cart cart) {
        this.cart = cart;
    }

    // Updates the quantity.
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
