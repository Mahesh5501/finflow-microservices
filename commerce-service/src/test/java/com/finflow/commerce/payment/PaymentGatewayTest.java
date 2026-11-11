package com.finflow.commerce.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Provider contract tests use real request construction with an in-process HTTP server stub. */
class PaymentGatewayTest {

    private MockRestServiceServer server;
    private WalletPaymentGateway gateway;

    @BeforeEach
    void createGateway() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://wallet.test");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new WalletPaymentGateway(builder.build(), new ObjectMapper());
    }

    @Test
    void chargeForwardsBearerAndStableKeyAndMapsReferences() {
        expectWallet("INR");
        server.expect(requestTo("http://wallet.test/api/v1/wallets/70/withdrawals"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer token"))
                .andExpect(header("Idempotency-Key", "commerce-pay-1"))
                .andExpect(content().json("{\"amount\":20.00}"))
                .andRespond(withSuccess("{\"id\":80,\"walletId\":70}", MediaType.APPLICATION_JSON));
        assertThat(gateway.charge(charge())).isEqualTo(PaymentResult.approved(70L, 80L));
        server.verify();
    }

    @Test
    void currencyMismatchDoesNotWithdraw() {
        expectWallet("USD");
        assertThat(gateway.charge(charge()).failureCode()).isEqualTo("CURRENCY_MISMATCH");
        server.verify();
    }

    @Test
    void businessRejectionIsDeclinedButServerErrorRemainsUncertain() {
        expectWallet("INR");
        server.expect(requestTo("http://wallet.test/api/v1/wallets/70/withdrawals"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"INSUFFICIENT_BALANCE\"}"));
        assertThat(gateway.charge(charge())).isEqualTo(PaymentResult.declined("INSUFFICIENT_BALANCE"));
        server.verify();

        server.reset();
        server.expect(requestTo("http://wallet.test/api/v1/users/900/wallet"))
                .andRespond(withServerError());
        assertThat(gateway.charge(charge())).isEqualTo(PaymentResult.uncertain("WALLET_UNAVAILABLE"));
        server.verify();
    }

    @Test
    void refundUsesRecordedWalletAndStableKey() {
        server.expect(requestTo("http://wallet.test/api/v1/wallets/70/deposits"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer token"))
                .andExpect(header("Idempotency-Key", "commerce-refund-1"))
                .andExpect(content().json("{\"amount\":20.00}"))
                .andRespond(withSuccess("{\"id\":81,\"walletId\":70}", MediaType.APPLICATION_JSON));
        assertThat(gateway.refund(new PaymentRefundRequest(900L, 70L, new BigDecimal("20.00"),
                "commerce-refund-1", "Bearer token"))).isEqualTo(PaymentResult.approved(70L, 81L));
        server.verify();
    }

    @Test
    void providerPropertiesStillSelectOneGatewayAndWalletClient() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(PaymentConfig.class, WalletPaymentGateway.class, MockPaymentGateway.class)
                .withBean(ObjectMapper.class, () -> new ObjectMapper());
        runner.withPropertyValues("payment.provider=simulated").run(context -> {
            assertThat(context).hasSingleBean(PaymentGateway.class).doesNotHaveBean(RestClient.class);
            assertThat(context.getBean(PaymentGateway.class)).isInstanceOf(MockPaymentGateway.class);
        });
        runner.withPropertyValues("payment.provider=wallet", "wallet.base-url=http://wallet.test").run(context -> {
            assertThat(context).hasSingleBean(PaymentGateway.class).hasSingleBean(RestClient.class);
            assertThat(context.getBean(PaymentGateway.class)).isInstanceOf(WalletPaymentGateway.class);
        });
        runner.run(context -> assertThat(context).hasSingleBean(PaymentGateway.class));
    }

    @Test
    void simulatorPreservesHintsAndDoesNotInventWalletReferences() {
        PaymentGateway mock = new MockPaymentGateway();
        PaymentResult approval = mock.charge(charge());
        assertThat(approval.outcome()).isEqualTo(PaymentOutcome.APPROVED);
        assertThat(approval.simulated()).isTrue();
        assertThat(approval.walletId()).isNull();
        assertThat(approval.walletTransactionId()).isNull();
        assertThat(PaymentResult.approved(70L, 80L).simulated()).isFalse();
        assertThat(mock.charge(new PaymentChargeRequest(900L, BigDecimal.TEN, "INR", "key", null,
                PaymentOutcome.UNCERTAIN)).outcome()).isEqualTo(PaymentOutcome.UNCERTAIN);
        PaymentResult refund = mock.refund(new PaymentRefundRequest(900L, 70L, BigDecimal.TEN, "key", null));
        assertThat(refund.outcome()).isEqualTo(PaymentOutcome.APPROVED);
        assertThat(refund.simulated()).isTrue();
        assertThat(refund.walletId()).isEqualTo(70L);
        assertThat(refund.walletTransactionId()).isNull();
    }

    private void expectWallet(String currency) {
        server.expect(requestTo("http://wallet.test/api/v1/users/900/wallet"))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("{\"walletId\":70,\"userId\":900,\"currency\":\"" + currency + "\"}",
                        MediaType.APPLICATION_JSON));
    }

    private static PaymentChargeRequest charge() {
        return new PaymentChargeRequest(900L, new BigDecimal("20.00"), "INR", "commerce-pay-1", "token", null);
    }
}
