package com.finflow.commerce.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Pays / refunds by calling wallet-service with the caller's Bearer JWT.
 * Idempotency-Key is stable per order so retries never double-debit.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "wallet")
@CircuitBreaker(name = "walletService")
public class WalletPaymentAdapter implements PaymentPort {

    private final RestClient walletRestClient;
    private final ObjectMapper objectMapper;

    public WalletPaymentAdapter(RestClient walletRestClient, ObjectMapper objectMapper) {
        this.walletRestClient = walletRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentResult charge(PaymentChargeRequest request) {
        requireToken(request.accessToken());
        try {
            WalletView wallet = getOwnWallet(request.customerUserId(), request.accessToken());
            if (!wallet.currency().equalsIgnoreCase(request.currencyCode())) {
                return PaymentResult.declined("CURRENCY_MISMATCH");
            }
            TransactionView tx = withdraw(wallet.walletId(), request.amount(), request.idempotencyKey(), request.accessToken());
            return PaymentResult.approved(wallet.walletId(), tx.id());
        } catch (WalletDeclineException ex) {
            return PaymentResult.declined(ex.code());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return PaymentResult.declined(readCode(ex));
            }
            // 5xx / transport ambiguity — keep stock reserved (ACTION_REQUIRED).
            return PaymentResult.uncertain("WALLET_UNAVAILABLE");
        } catch (Exception ex) {
            return PaymentResult.uncertain("WALLET_UNAVAILABLE");
        }
    }

    @Override
    public PaymentResult refund(PaymentRefundRequest request) {
        requireToken(request.accessToken());
        Long walletId = request.walletId();
        try {
            if (walletId == null) {
                walletId = getOwnWallet(request.customerUserId(), request.accessToken()).walletId();
            }
            TransactionView tx = deposit(walletId, request.amount(), request.idempotencyKey(), request.accessToken());
            return PaymentResult.approved(walletId, tx.id());
        } catch (WalletDeclineException ex) {
            return PaymentResult.declined(ex.code());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return PaymentResult.declined(readCode(ex));
            }
            return PaymentResult.uncertain("WALLET_UNAVAILABLE");
        } catch (Exception ex) {
            return PaymentResult.uncertain("WALLET_UNAVAILABLE");
        }
    }

    private WalletView getOwnWallet(Long userId, String accessToken) {
        try {
            return walletRestClient.get()
                    .uri("/api/v1/users/{userId}/wallet", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .body(WalletView.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new WalletDeclineException("WALLET_NOT_FOUND");
            }
            throw ex;
        }
    }

    private TransactionView withdraw(Long walletId, BigDecimal amount, String idempotencyKey, String accessToken) {
        try {
            return walletRestClient.post()
                    .uri("/api/v1/wallets/{walletId}/withdrawals", walletId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amount))
                    .retrieve()
                    .body(TransactionView.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 422) {
                throw new WalletDeclineException(readCode(ex));
            }
            throw ex;
        }
    }

    private TransactionView deposit(Long walletId, BigDecimal amount, String idempotencyKey, String accessToken) {
        return walletRestClient.post()
                .uri("/api/v1/wallets/{walletId}/deposits", walletId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .body(TransactionView.class);
    }

    private String readCode(RestClientResponseException ex) {
        try {
            JsonNode node = objectMapper.readTree(ex.getResponseBodyAsString());
            if (node.hasNonNull("code")) {
                return node.get("code").asText();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "WALLET_REJECTED";
    }

    private static void requireToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new WalletDeclineException("MISSING_ACCESS_TOKEN");
        }
    }

    private static String bearer(String accessToken) {
        return accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
    }

    /** Minimal wallet wire model — only fields commerce needs. */
    public record WalletView(Long walletId, Long userId, String currency) {
    }

    public record TransactionView(Long id, Long walletId) {
    }

    private static final class WalletDeclineException extends RuntimeException {
        private final String code;

        private WalletDeclineException(String code) {
            super(code);
            this.code = code;
        }

        private String code() {
            return code;
        }
    }
}
