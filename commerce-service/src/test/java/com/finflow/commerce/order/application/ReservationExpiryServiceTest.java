package com.finflow.commerce.order.application;

import com.finflow.commerce.cart.dto.CartItemRequest;
import com.finflow.commerce.cart.application.CartService;
import com.finflow.commerce.cart.application.AbandonedCartCleanupService;
import com.finflow.commerce.catalogue.application.CatalogueService;
import com.finflow.commerce.order.dto.CreateOrderRequest;
import com.finflow.commerce.order.dto.OrderItemRequest;
import com.finflow.commerce.order.dto.OrderResponse;
import com.finflow.commerce.catalogue.dto.ProductRequest;
import com.finflow.commerce.catalogue.dto.ProductResponse;
import com.finflow.commerce.order.domain.OrderStatus;
import com.finflow.commerce.order.domain.OutboxStatus;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.cart.persistence.CartRepository;
import com.finflow.commerce.order.persistence.PaymentOutboxRepository;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ReservationExpiryServiceTest {

    private static final List<Long> ORGS = List.of(10L);

    @Autowired
    private CommerceService commerceService;

    @Autowired
    private CatalogueService catalogueService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ReservationExpiryService reservationExpiryService;

    @Autowired
    private AbandonedCartCleanupService abandonedCartCleanupService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Autowired
    private CartRepository cartRepository;

    @Test
    void expireStale_releasesStockAndMarksExpired() {
        ProductResponse product = catalogueService.createProduct(
                ORGS, new ProductRequest("TTL Item", new BigDecimal("10.00"), 5, null)
        );

        OrderResponse order = commerceService.createOrder(
                55L,
                ORGS,
                "ord-ttl-1",
                null,
                new CreateOrderRequest(
                        10L,
                        List.of(new OrderItemRequest(product.productId(), 2)),
                        null,
                        null,
                        PaymentOutcome.UNCERTAIN
                )
        );
        assertEquals(OrderStatus.ACTION_REQUIRED, order.status());
        assertEquals(3, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());

        int expired = reservationExpiryService.expireStaleReservations(Instant.now().plus(1, ChronoUnit.MINUTES));
        assertTrue(expired >= 1);

        assertEquals(5, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());
        assertEquals(OrderStatus.EXPIRED, commerceService.getOrder(order.orderId(), 77L, ORGS).status());
        assertEquals(OutboxStatus.FAILED, outboxRepository.findByOrderId(order.orderId()).orElseThrow().getStatus());
    }

    @Test
    void purgeAbandonedCarts_removesStaleCarts() {
        ProductResponse product = catalogueService.createProduct(
                ORGS, new ProductRequest("Cart Dust", new BigDecimal("8.00"), 4, null)
        );
        cartService.addItem(77L, new CartItemRequest(product.productId(), 1));
        assertTrue(cartRepository.findByCustomerUserIdAndOrganisationId(77L, 10L).isPresent());

        int purged = abandonedCartCleanupService.purgeAbandonedCarts(Instant.now().plus(1, ChronoUnit.HOURS));
        assertTrue(purged >= 1);
        assertTrue(cartRepository.findByCustomerUserIdAndOrganisationId(77L, 10L).isEmpty());
    }
}
