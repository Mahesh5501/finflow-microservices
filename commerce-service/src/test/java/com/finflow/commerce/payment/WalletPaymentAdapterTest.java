package com.finflow.commerce.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletPaymentAdapterTest {

    private MockRestServiceServer server;
    private WalletPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://wallet.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new WalletPaymentAdapter(builder.build(), new ObjectMapper());
    }

    @Test
    void charge_withdrawsFromCallerWallet() {
        server.expect(requestTo("http://wallet.test/api/v1/users/99/wallet"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess(
                        "{\"walletId\":7,\"userId\":99,\"currency\":\"INR\"}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo("http://wallet.test/api/v1/wallets/7/withdrawals"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "commerce-pay-1"))
                .andRespond(withSuccess(
                        "{\"id\":55,\"walletId\":7}",
                        MediaType.APPLICATION_JSON
                ));

        PaymentResult result = adapter.charge(new PaymentChargeRequest(
                99L, new BigDecimal("100.00"), "INR", "commerce-pay-1", "tok", null
        ));

        assertEquals(PaymentOutcome.APPROVED, result.outcome());
        assertEquals(7L, result.walletId());
        assertEquals(55L, result.walletTransactionId());
        server.verify();
    }

    @Test
    void charge_insufficientBalance_declines() {
        server.expect(requestTo("http://wallet.test/api/v1/users/99/wallet"))
                .andRespond(withSuccess(
                        "{\"walletId\":7,\"userId\":99,\"currency\":\"INR\"}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo("http://wallet.test/api/v1/wallets/7/withdrawals"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"INSUFFICIENT_BALANCE\",\"message\":\"low\"}"));

        PaymentResult result = adapter.charge(new PaymentChargeRequest(
                99L, new BigDecimal("100.00"), "INR", "commerce-pay-2", "Bearer tok", null
        ));

        assertEquals(PaymentOutcome.DECLINED, result.outcome());
        assertEquals("INSUFFICIENT_BALANCE", result.failureCode());
        assertNull(result.walletTransactionId());
        server.verify();
    }

    @Test
    void refund_depositsBack() {
        server.expect(requestTo("http://wallet.test/api/v1/wallets/7/deposits"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "commerce-refund-1"))
                .andRespond(withSuccess("{\"id\":90,\"walletId\":7}", MediaType.APPLICATION_JSON));

        PaymentResult result = adapter.refund(new PaymentRefundRequest(
                99L, 7L, new BigDecimal("100.00"), "commerce-refund-1", "tok"
        ));

        assertEquals(PaymentOutcome.APPROVED, result.outcome());
        assertEquals(90L, result.walletTransactionId());
        server.verify();
    }
}
