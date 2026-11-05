package com.finflow.commerce.service;

import com.finflow.commerce.dto.CartItemRequest;
import com.finflow.commerce.dto.CartResponse;
import com.finflow.commerce.dto.CheckoutRequest;
import com.finflow.commerce.dto.OrderResponse;
import com.finflow.commerce.dto.ProductRequest;
import com.finflow.commerce.dto.ProductResponse;
import com.finflow.commerce.dto.ShippingAddressRequest;
import com.finflow.commerce.dto.UpdateCartItemRequest;
import com.finflow.commerce.entity.OrderStatus;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CartServiceTest {

    private static final List<Long> ORGS = List.of(10L);
    private static final ShippingAddressRequest SHIP = new ShippingAddressRequest(
            "1 Park Ave", "Apt 2", "Mumbai", "MH", "400001", "IN"
    );

    @Autowired
    private CartService cartService;

    @Autowired
    private CommerceService commerceService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void addUpdateCheckout_clearsCartAndConfirmsOrder() {
        ProductResponse product = commerceService.createProduct(
                1L, ORGS, new ProductRequest(10L, "Lamp", new BigDecimal("25.00"), 8, null)
        );

        cartService.addItem(42L, ORGS, new CartItemRequest(10L, product.productId(), 2));
        CartResponse cart = cartService.upsertItem(
                42L, ORGS, product.productId(), new UpdateCartItemRequest(10L, 3)
        );
        assertEquals(1, cart.items().size());
        assertEquals(3, cart.items().getFirst().quantity());
        assertEquals(0, new BigDecimal("75.00").compareTo(cart.subtotal()));

        OrderResponse order = cartService.checkout(
                42L,
                ORGS,
                "cart-checkout-1",
                null,
                new CheckoutRequest(10L, SHIP, null, PaymentOutcome.APPROVED)
        );

        assertEquals(OrderStatus.CONFIRMED, order.status());
        assertEquals(5, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());
        assertTrue(cartService.getCart(42L, ORGS, 10L).items().isEmpty());
    }
}
