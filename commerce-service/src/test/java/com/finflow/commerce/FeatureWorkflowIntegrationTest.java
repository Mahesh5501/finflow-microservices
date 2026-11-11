package com.finflow.commerce;

import com.finflow.commerce.cart.application.CartService;
import com.finflow.commerce.cart.dto.CartItemRequest;
import com.finflow.commerce.catalogue.application.CatalogueService;
import com.finflow.commerce.catalogue.dto.ProductRequest;
import com.finflow.commerce.catalogue.dto.ProductResponse;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import com.finflow.commerce.coupon.application.CouponService;
import com.finflow.commerce.coupon.dto.CouponRequest;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.integration.event.CommerceEventPublisher;
import com.finflow.commerce.order.application.CommerceService;
import com.finflow.commerce.order.application.PaymentSettlementService;
import com.finflow.commerce.order.domain.CommerceOrder;
import com.finflow.commerce.order.domain.OrderStatus;
import com.finflow.commerce.order.domain.OutboxStatus;
import com.finflow.commerce.order.dto.CheckoutRequest;
import com.finflow.commerce.order.dto.CreateOrderRequest;
import com.finflow.commerce.order.dto.OrderItemRequest;
import com.finflow.commerce.order.dto.OrderResponse;
import com.finflow.commerce.order.dto.ShippingAddressRequest;
import com.finflow.commerce.order.persistence.CommerceOrderRepository;
import com.finflow.commerce.order.persistence.PaymentOutboxRepository;
import com.finflow.commerce.payment.PaymentChargeRequest;
import com.finflow.commerce.payment.PaymentGateway;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.payment.PaymentRefundRequest;
import com.finflow.commerce.payment.PaymentResult;
import com.finflow.commerce.returns.application.ReturnService;
import com.finflow.commerce.returns.domain.ReturnStatus;
import com.finflow.commerce.returns.dto.ReturnRequestBody;
import com.finflow.commerce.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises actual feature wiring, H2 persistence, transactions and MVC security.
 * HTTP principals are supplied by the security test helper; live JWKS verification,
 * Redis/MySQL concurrency and Kafka delivery require separate environment checks.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeatureWorkflowIntegrationTest {

    private static final long SELLER = 501L;
    private static final long BUYER = 900L;
    private static final List<Long> MERCHANT_ORGS = List.of(SELLER);
    @Autowired private CatalogueService catalogue;
    @Autowired private CouponService coupons;
    @Autowired private CartService carts;
    @Autowired private CommerceService orders;
    @Autowired private ReturnService returns;
    @Autowired private ProductRepository products;
    @Autowired private CommerceOrderRepository orderRepository;
    @Autowired private PaymentOutboxRepository outbox;
    @Autowired private TransactionTemplate transactions;
    @Autowired private CommerceEventPublisher events;
    @Autowired private MockMvc mvc;

    @Test
    void couponCheckoutForBuyerOutsideSellerOrgCommitsReservationAndClearsCart() {
        ProductResponse product = product();
        String code = "C" + UUID.randomUUID().toString().replace("-", "");
        var createdCoupon = coupons.createCoupon(MERCHANT_ORGS,
                new CouponRequest(SELLER, code, new BigDecimal("10"), null));
        carts.clear(BUYER, List.of());
        carts.addItem(BUYER, List.of(), new CartItemRequest(product.productId(), 2));
        OrderResponse result = carts.checkout(BUYER, List.of(), key(), null,
                new CheckoutRequest(null, address(), code.toLowerCase(), PaymentOutcome.UNCERTAIN), SELLER);

        assertEquals(OrderStatus.ACTION_REQUIRED, result.status());
        assertEquals(0, new BigDecimal("180.0000").compareTo(result.totalAmount()));
        assertEquals(0, new BigDecimal("20.0000").compareTo(result.discountAmount()));
        assertEquals(createdCoupon.code(), result.couponCode());
        assertEquals(8, stock(product));
        assertEquals(OutboxStatus.PENDING, outbox.findByOrderId(result.orderId()).orElseThrow().getStatus());
        assertTrue(carts.getCart(BUYER).items().isEmpty());
        assertEquals("IN", result.shippingAddress().country());
    }

    @Test
    void replayKeepsOneOrderAndReservationAndRejectsChangedOwnerOrPayload() {
        ProductResponse product = product();
        String key = key();
        CreateOrderRequest request = request(product, null, PaymentOutcome.UNCERTAIN);
        OrderResponse first = orders.createOrder(BUYER, List.of(), key, null, request);
        OrderResponse replay = orders.createOrder(BUYER, List.of(), key, null, request);
        assertEquals(first.orderId(), replay.orderId());
        assertEquals(8, stock(product));
        assertEquals("IDEMPOTENCY_KEY_CONFLICT", assertThrows(ApiException.class,
                () -> orders.createOrder(BUYER + 1, List.of(), key, null, request)).getCode());
        CreateOrderRequest changed = new CreateOrderRequest(SELLER,
                List.of(new OrderItemRequest(product.productId(), 3)), null, null, PaymentOutcome.UNCERTAIN);
        assertEquals("IDEMPOTENCY_KEY_CONFLICT", assertThrows(ApiException.class,
                () -> orders.createOrder(BUYER, List.of(), key, null, changed)).getCode());
        assertEquals(8, stock(product));
    }

    @Test
    void invalidCouponRollsBackStockAndOrderButDeclineCommitsFailedWork() {
        ProductResponse product = product();
        long ordersBefore = orderRepository.count();
        assertEquals("COUPON_NOT_FOUND", assertThrows(ApiException.class,
                () -> orders.createOrder(BUYER, List.of(), key(), null,
                        request(product, key(), PaymentOutcome.UNCERTAIN))).getCode());
        assertEquals(10, stock(product));
        assertEquals(ordersBefore, orderRepository.count());

        OrderResponse declined = orders.createOrder(BUYER, List.of(), key(), null,
                request(product, null, PaymentOutcome.DECLINED));
        assertEquals(OrderStatus.PAYMENT_FAILED, declined.status());
        assertEquals(10, stock(product));
        assertEquals(OutboxStatus.FAILED, outbox.findByOrderId(declined.orderId()).orElseThrow().getStatus());
    }

    @Test
    void expiredCouponAndForeignSellerCouponDoNotReserveStock() {
        ProductResponse product = product();
        String expired = key();
        coupons.createCoupon(MERCHANT_ORGS,
                new CouponRequest(SELLER, expired, BigDecimal.TEN, Instant.now().minusSeconds(60)));
        assertEquals("COUPON_INVALID", assertThrows(ApiException.class,
                () -> orders.createOrder(BUYER, List.of(), key(), null,
                        request(product, expired, PaymentOutcome.UNCERTAIN))).getCode());
        String foreign = key();
        coupons.createCoupon(List.of(502L), new CouponRequest(502L, foreign, BigDecimal.TEN, null));
        assertEquals("COUPON_NOT_FOUND", assertThrows(ApiException.class,
                () -> orders.createOrder(BUYER, List.of(), key(), null,
                        request(product, foreign, PaymentOutcome.UNCERTAIN))).getCode());
        assertEquals(10, stock(product));
    }

    @Test
    void approvedSettlementCallsGatewayOutsideTransactionAndRecordsReferences() {
        OrderResponse pending = orders.createOrder(BUYER, List.of(), key(), null,
                request(product(), null, PaymentOutcome.UNCERTAIN));
        // Explicit provider fixture: an approved real-provider result includes references.
        PaymentGateway approved = new PaymentGateway() {
            @Override
            public PaymentResult charge(PaymentChargeRequest request) {
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                assertEquals("commerce-pay-" + pending.orderId(), request.idempotencyKey());
                assertTrue(orderRepository.findById(pending.orderId()).isPresent());
                return PaymentResult.approved(70L, 80L);
            }

            @Override
            public PaymentResult refund(PaymentRefundRequest request) {
                throw new AssertionError("No refund expected during charge");
            }
        };
        PaymentSettlementService settlement = new PaymentSettlementService(
                orderRepository, outbox, products, approved, transactions, events);
        CommerceOrder result = settlement.settleCharge(pending.orderId(), "test-token");
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertEquals(70L, result.getPaymentWalletId());
        assertEquals(80L, result.getPaymentTransactionId());
        assertEquals(OutboxStatus.DONE, outbox.findByOrderId(pending.orderId()).orElseThrow().getStatus());
    }

    @Test
    void cancellationAndReturnsKeepOwnerAndMerchantBoundaries() {
        ProductResponse product = product();
        OrderResponse pending = orders.createOrder(BUYER, List.of(), key(), null,
                request(product, null, PaymentOutcome.UNCERTAIN));
        assertThrows(ApiException.class,
                () -> orders.cancelOrder(pending.orderId(), BUYER + 1, MERCHANT_ORGS, null));
        assertEquals(OrderStatus.CANCELLED,
                orders.cancelOrder(pending.orderId(), BUYER, List.of(), null).status());
        assertEquals(10, stock(product));

        OrderResponse returnable = orders.createOrder(BUYER, List.of(), key(), null,
                request(product, null, PaymentOutcome.UNCERTAIN));
        // Seed a paid state for the return workflow; this test does not prove simulator charge success.
        transactions.executeWithoutResult(tx -> {
            CommerceOrder order = orderRepository.findById(returnable.orderId()).orElseThrow();
            order.recordPayment(70L, 80L);
            order.transitionTo(OrderStatus.CONFIRMED);
            order.transitionTo(OrderStatus.FULFILLED);
        });
        assertThrows(ApiException.class, () -> returns.requestReturn(
                returnable.orderId(), BUYER + 1, MERCHANT_ORGS, new ReturnRequestBody("wrong buyer")));
        var requested = returns.requestReturn(returnable.orderId(), BUYER, List.of(),
                new ReturnRequestBody("damaged"));
        assertThrows(ApiException.class, () -> returns.approve(requested.returnId(), List.of(502L)));
        assertEquals(ReturnStatus.APPROVED, returns.approve(requested.returnId(), MERCHANT_ORGS).status());
        assertEquals(ReturnStatus.COMPLETED, returns.complete(requested.returnId(), MERCHANT_ORGS, null).status());
        assertEquals(10, stock(product));
        assertEquals(OrderStatus.FULFILLED, orders.getOrder(returnable.orderId(), BUYER, List.of()).status());
        assertThrows(ApiException.class, () -> returns.complete(requested.returnId(), MERCHANT_ORGS, null));
        assertEquals(10, stock(product));
    }

    @Test
    void migratedControllersKeepRoutesJsonValidationAndSecurity() throws Exception {
        mvc.perform(get("/api/v1/customer/products/search")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/merchant/coupons").with(authentication(auth(BUYER, List.of(), "commerce:read")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/customer/carts").with(authentication(auth(BUYER, List.of(), "commerce:read"))))
                .andExpect(status().isOk()).andExpect(jsonPath("customerUserId").value(BUYER))
                .andExpect(jsonPath("items").isArray());
        mvc.perform(post("/api/v1/customer/carts/items").with(authentication(auth(BUYER, List.of(), "commerce:write")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest());
        String body = "{\"organisationId\":501,\"code\":\"" + key() + "\",\"percentOff\":10}";
        mvc.perform(post("/api/v1/merchant/coupons").with(authentication(auth(100L, MERCHANT_ORGS, "commerce:write")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("organisationId").value(SELLER))
                .andExpect(jsonPath("active").value(true));
        mvc.perform(post("/api/v1/merchant/coupons").with(authentication(auth(100L, List.of(502L), "commerce:write")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/merchant/orders").param("organisationId", "501")
                        .with(authentication(auth(100L, List.of(502L), "commerce:read"))))
                .andExpect(status().isForbidden());
    }

    private ProductResponse product() {
        return catalogue.createProduct(MERCHANT_ORGS,
                new ProductRequest(SELLER, "Workflow " + key(), new BigDecimal("100.00"), 10, null));
    }

    private int stock(ProductResponse product) {
        return products.findById(product.productId()).orElseThrow().getAvailableQuantity();
    }

    private static CreateOrderRequest request(ProductResponse product, String coupon, PaymentOutcome hint) {
        return new CreateOrderRequest(SELLER, List.of(new OrderItemRequest(product.productId(), 2)), null, coupon, hint);
    }

    private static ShippingAddressRequest address() {
        return new ShippingAddressRequest("1 High Street", null, "Hyderabad", "Telangana", "500001", "IN");
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private static UsernamePasswordAuthenticationToken auth(long userId, List<Long> orgs, String permission) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "test@example.com", orgs, List.of(permission)), null,
                AuthorityUtils.createAuthorityList(permission));
    }
}
