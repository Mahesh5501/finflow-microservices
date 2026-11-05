package com.finflow.commerce.service;

import com.finflow.commerce.dto.CouponRequest;
import com.finflow.commerce.dto.CreateOrderRequest;
import com.finflow.commerce.dto.OrderItemRequest;
import com.finflow.commerce.dto.OrderResponse;
import com.finflow.commerce.dto.ProductRequest;
import com.finflow.commerce.dto.ProductResponse;
import com.finflow.commerce.dto.ProductUpdateRequest;
import com.finflow.commerce.dto.ShippingAddressRequest;
import com.finflow.commerce.entity.OrderStatus;
import com.finflow.commerce.entity.OutboxStatus;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.repository.PaymentOutboxRepository;
import com.finflow.commerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CommerceServiceTest {

    private static final List<Long> ORGS = List.of(10L);
    private static final ShippingAddressRequest SHIP = new ShippingAddressRequest(
            "12 MG Road", null, "Bengaluru", "KA", "560001", "IN"
    );

    @Autowired
    private CommerceService commerceService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Test
    void createOrder_snapshotsPrice_andConfirmsOnApprovedPayment() {
        ProductResponse product = commerceService.createProduct(
                1L, ORGS, new ProductRequest(10L, "Tea", new BigDecimal("50.00"), 5, null)
        );

        OrderResponse order = commerceService.createOrder(
                99L,
                ORGS,
                "ord-1",
                null,
                new CreateOrderRequest(10L, List.of(new OrderItemRequest(product.productId(), 2)), SHIP, null, PaymentOutcome.APPROVED)
        );

        assertEquals(OrderStatus.CONFIRMED, order.status());
        assertEquals(0, new BigDecimal("100.00").compareTo(order.totalAmount()));
        assertEquals(3, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());
        assertNotNull(order.shippingAddress());
        assertEquals(OutboxStatus.DONE, outboxRepository.findByOrderId(order.orderId()).orElseThrow().getStatus());
    }

    @Test
    void createOrder_idempotentReplay_returnsSameOrder() {
        ProductResponse product = commerceService.createProduct(
                1L, ORGS, new ProductRequest(10L, "Mug", new BigDecimal("20.00"), 10, null)
        );
        CreateOrderRequest req = new CreateOrderRequest(
                10L, List.of(new OrderItemRequest(product.productId(), 1)), SHIP, null, PaymentOutcome.APPROVED
        );

        OrderResponse first = commerceService.createOrder(99L, ORGS, "ord-idem", null, req);
        OrderResponse second = commerceService.createOrder(99L, ORGS, "ord-idem", null, req);

        assertEquals(first.orderId(), second.orderId());
        assertEquals(9, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());
    }

    @Test
    void createOrder_declinedPayment_releasesStock() {
        ProductResponse product = commerceService.createProduct(
                1L, ORGS, new ProductRequest(10L, "Cup", new BigDecimal("15.00"), 4, null)
        );

        OrderResponse order = commerceService.createOrder(
                99L,
                ORGS,
                "ord-decline",
                null,
                new CreateOrderRequest(10L, List.of(new OrderItemRequest(product.productId(), 2)), SHIP, null, PaymentOutcome.DECLINED)
        );

        assertEquals(OrderStatus.PAYMENT_FAILED, order.status());
        assertEquals(4, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());
        assertEquals(OutboxStatus.FAILED, outboxRepository.findByOrderId(order.orderId()).orElseThrow().getStatus());
    }

    @Test
    void createOrder_uncertainPayment_keepsStockReserved() {
        ProductResponse product = commerceService.createProduct(
                1L, ORGS, new ProductRequest(10L, "Jar", new BigDecimal("30.00"), 3, null)
        );

        OrderResponse order = commerceService.createOrder(
                99L,
                ORGS,
                "ord-uncertain",
                null,
                new CreateOrderRequest(10L, List.of(new OrderItemRequest(product.productId(), 1)), null, null, PaymentOutcome.UNCERTAIN)
        );

        assertEquals(OrderStatus.ACTION_REQUIRED, order.status());
        assertEquals(2, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());
    }

    @Test
    void createOrder_insufficientStock_rejected() {
        ProductResponse product = commerceService.createProduct(
                1L, ORGS, new ProductRequest(10L, "Rare", new BigDecimal("99.00"), 1, null)
        );

        ApiException ex = assertThrows(ApiException.class, () -> commerceService.createOrder(
                99L,
                ORGS,
                "ord-stock",
                null,
                new CreateOrderRequest(10L, List.of(new OrderItemRequest(product.productId(), 5)), SHIP, null, null)
        ));
        assertEquals("INSUFFICIENT_STOCK", ex.getCode());
    }

    @Test
    void createOrder_crossOrg_forbidden() {
        assertThrows(ApiException.class, () -> commerceService.createProduct(
                1L, ORGS, new ProductRequest(99L, "X", new BigDecimal("1.00"), 1, null)
        ));
    }

    @Test
    void cancelConfirmed_refundsAndRestocks() {
        ProductResponse product = commerceService.createProduct(
                1L, ORGS, new ProductRequest(10L, "Bag", new BigDecimal("40.00"), 6, null)
        );

        OrderResponse order = commerceService.createOrder(
                99L,
                ORGS,
                "ord-cancel-paid",
                null,
                new CreateOrderRequest(10L, List.of(new OrderItemRequest(product.productId(), 2)), SHIP, null, PaymentOutcome.APPROVED)
        );
        assertEquals(OrderStatus.CONFIRMED, order.status());
        assertEquals(4, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());

        OrderResponse cancelled = commerceService.cancelOrder(order.orderId(), 99L, ORGS, null);
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(6, productRepository.findById(product.productId()).orElseThrow().getAvailableQuantity());
    }

    @Test
    void coupon_and_fulfilment_flow() {
        ProductResponse product = commerceService.createProduct(
                1L, ORGS, new ProductRequest(10L, "Hoodie", new BigDecimal("100.00"), 5, null)
        );
        commerceService.createCoupon(ORGS, new CouponRequest(10L, "SAVE10", new BigDecimal("10.00"), null));
        commerceService.updateProduct(product.productId(), ORGS, new ProductUpdateRequest(new BigDecimal("100.00"), 5, null));

        OrderResponse order = commerceService.createOrder(
                99L,
                ORGS,
                "ord-coupon",
                null,
                new CreateOrderRequest(
                        10L,
                        List.of(new OrderItemRequest(product.productId(), 1)),
                        SHIP,
                        "SAVE10",
                        PaymentOutcome.APPROVED
                )
        );
        assertEquals(0, new BigDecimal("90.00").compareTo(order.totalAmount()));
        assertEquals(0, new BigDecimal("10.00").compareTo(order.discountAmount()));
        assertEquals("SAVE10", order.couponCode());

        OrderResponse shipping = commerceService.startFulfilment(order.orderId(), ORGS);
        assertEquals(OrderStatus.FULFILMENT_PENDING, shipping.status());
        OrderResponse done = commerceService.markFulfilled(order.orderId(), ORGS);
        assertEquals(OrderStatus.FULFILLED, done.status());
    }
}
