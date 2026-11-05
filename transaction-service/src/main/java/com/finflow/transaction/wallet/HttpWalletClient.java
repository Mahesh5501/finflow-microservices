package com.finflow.transaction.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "wallet.provider", havingValue = "wallet")
public class HttpWalletClient implements WalletClient {

    private final RestClient walletHttpClient;
    private final ObjectMapper objectMapper;

    public HttpWalletClient(RestClient walletHttpClient, ObjectMapper objectMapper) {
        this.walletHttpClient = walletHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public WalletAccount getWallet(Long walletId, String accessToken) {
        try {
            return walletHttpClient.get()
                    .uri("/api/v1/wallets/{walletId}", walletId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .body(WalletAccount.class);
        } catch (RestClientResponseException ex) {
            throw map(ex);
        }
    }

    @Override
    public void deposit(Long walletId, BigDecimal amount, String idempotencyKey, String accessToken) {
        postMoney(walletId, "deposits", amount, idempotencyKey, accessToken);
    }

    @Override
    public void withdraw(Long walletId, BigDecimal amount, String idempotencyKey, String accessToken) {
        postMoney(walletId, "withdrawals", amount, idempotencyKey, accessToken);
    }

    private void postMoney(
            Long walletId,
            String path,
            BigDecimal amount,
            String idempotencyKey,
            String accessToken
    ) {
        try {
            walletHttpClient.post()
                    .uri("/api/v1/wallets/{walletId}/{path}", walletId, path)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw map(ex);
        }
    }

    private RuntimeException map(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String code = readCode(ex);
        String message = readMessage(ex);
        if (status == 422) {
            return new WalletClientException(code, message);
        }
        if (status == 403) {
            return new WalletClientException("FORBIDDEN", message);
        }
        if (status == 404) {
            return new WalletClientException("WALLET_NOT_FOUND", message);
        }
        return ex;
    }

    private String readCode(RestClientResponseException ex) {
        try {
            JsonNode node = objectMapper.readTree(ex.getResponseBodyAsString());
            return node.path("code").asText("WALLET_ERROR");
        } catch (Exception ignored) {
            return "WALLET_ERROR";
        }
    }

    private String readMessage(RestClientResponseException ex) {
        try {
            JsonNode node = objectMapper.readTree(ex.getResponseBodyAsString());
            String msg = node.path("message").asText(null);
            return msg != null ? msg : ex.getMessage();
        } catch (Exception ignored) {
            return ex.getMessage();
        }
    }

    private static String bearer(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Authorization bearer token required for wallet calls");
        }
        return accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
    }
}
