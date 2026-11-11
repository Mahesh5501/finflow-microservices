package com.finflow.commerce.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Real payment adapter that communicates with Wallet Service over HTTP.
 * Commerce Service does not directly access Wallet Service's database. It communicates through Wallet Service APIs.
 * This adapter: 1. Finds the customer's wallet. 2. Checks the wallet currency. 3. Calls the withdrawal API for charges. 4. Calls the deposit API for refunds. 5. Converts wallet responses into PaymentResult.
 * Spring activates this adapter only when: payment.provider=wallet
 *
 * Payment infrastructure adapter called through PaymentGateway by order settlement.
 * It depends on the configured RestClient and ObjectMapper, injected explicitly so
 * tests can use MockRestServiceServer. Wallet owns balance/ledger/debit/credit; this
 * class only performs HTTP and normalizes responses. Removing @Component (without
 * @Bean registration) loses discovery; @ConditionalOnProperty selects this one provider
 * and prevents competing implementations. The provider property remains wallet.
 *
 * @CircuitBreaker/@Retry install Resilience4j proxy advice; removing them removes
 * interception, not HTTP itself. The current broad catch converts most failures to
 * UNCERTAIN before advice sees them, so configured retry/backoff and circuit opening
 * are not guaranteed. No fallback is configured for an already-open circuit. These
 * are existing limitations, not repaired by renaming. RestClient timeouts remain
 * 3s connect/10s read; stable keys go to the wallet for charges and refunds.
 */
@Component
@ConditionalOnProperty(
        name = "payment.provider",
        havingValue = "wallet"
)
@CircuitBreaker(name = "walletService")
@Retry(name = "walletService")
public class WalletPaymentGateway implements PaymentGateway {

    /*
     * RestClient is Spring's synchronous HTTP client.
     * We use it to call Wallet Service endpoints. It is configured with the Wallet Service base URL in PaymentConfig.
     */
    private final RestClient walletRestClient;

    /*
     * ObjectMapper converts JSON response bodies into a JSON tree.
     * We use it to extract a machine-readable error code from Wallet Service error responses.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor injection.
     * Spring supplies the configured RestClient and ObjectMapper. Using final fields ensures that these dependencies cannot be reassigned after construction.
     */
    public WalletPaymentGateway(
            RestClient walletRestClient,
            ObjectMapper objectMapper
    ) {
        this.walletRestClient = walletRestClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Charges the customer's wallet.
     * Flow: 1. Validate the caller's access token. 2. Find the customer's wallet. 3. Verify the wallet currency. 4. Call the wallet withdrawal endpoint. 5. Convert the response into PaymentResult.
     */
    @Override
    public PaymentResult charge(PaymentChargeRequest request) {

        // Wallet Service requires an authenticated caller.
        requireToken(request.accessToken());

        try {

            // Resolve the customer's wallet using their user ID.
            WalletView wallet = getOwnWallet(
                    request.customerUserId(),
                    request.accessToken()
            );

            // Never charge a wallet in a different currency.
            // For example, an INR order must not debit a USD wallet.
            if (!wallet.currency().equalsIgnoreCase(request.currencyCode())) {
                return PaymentResult.declined("CURRENCY_MISMATCH");
            }

            // Perform the actual wallet withdrawal.
            // The same idempotency key must be reused for retries of this logical payment operation.
            TransactionView tx = withdraw(
                    wallet.walletId(),
                    request.amount(),
                    request.idempotencyKey(),
                    request.accessToken()
            );

            // Convert the wallet transaction response into our provider-independent payment result.
            return PaymentResult.approved(
                    wallet.walletId(),
                    tx.id()
            );

        } catch (WalletDeclineException ex) {

            // Known business rejection, such as wallet not found or insufficient balance.
            return PaymentResult.declined(ex.code());

        } catch (RestClientResponseException ex) {

            // A 4xx response represents a client/business rejection according to this adapter's current contract.
            if (ex.getStatusCode().is4xxClientError()) {
                return PaymentResult.declined(readCode(ex));
            }

            // A server-side response may be ambiguous. Do not automatically release stock because the wallet may already have processed the request.
            return PaymentResult.uncertain("WALLET_UNAVAILABLE");

        } catch (Exception ex) {

            // Network errors, response conversion failures and other unexpected exceptions are currently normalized as uncertain. This is deliberately not treated as a definite decline.
            return PaymentResult.uncertain("WALLET_UNAVAILABLE");
        }
    }

    /**
     * Refunds money to the customer's wallet.
     * Flow: 1. Validate the caller's token. 2. Use the supplied wallet ID if available. 3. Otherwise resolve the customer's wallet. 4. Call the wallet deposit endpoint. 5. Convert the response into PaymentResult.
     */
    @Override
    public PaymentResult refund(PaymentRefundRequest request) {

        // Authentication is required for the wallet API call.
        requireToken(request.accessToken());
        // Prefer the wallet ID recorded during the original payment.
        Long walletId = request.walletId();
        try {
            // If the original wallet ID is unavailable, look up the customer's wallet.
            if (walletId == null) {
                walletId = getOwnWallet(
                        request.customerUserId(),
                        request.accessToken()
                ).walletId();
            }

            // Deposit the refund amount into the wallet.
            TransactionView tx = deposit(
                    walletId,
                    request.amount(),
                    request.idempotencyKey(),
                    request.accessToken()
            );

            // Return the normalized refund result.
            return PaymentResult.approved(walletId, tx.id());

        } catch (WalletDeclineException ex) {

            // Known wallet business rejection.
            return PaymentResult.declined(ex.code());

        } catch (RestClientResponseException ex) {

            // Treat client errors as known rejections under the current Wallet Service API contract.
            if (ex.getStatusCode().is4xxClientError()) {
                return PaymentResult.declined(readCode(ex));
            }

            // Server error: the refund result may be unknown. Server error: the refund result may be unknown.
            return PaymentResult.uncertain("WALLET_UNAVAILABLE");

        } catch (Exception ex) {

            // Unknown result: reconciliation may be required. Unknown result: reconciliation may be required.
            return PaymentResult.uncertain("WALLET_UNAVAILABLE");
        }
    }

    /**
     * Retrieves the wallet belonging to a customer.
     * GET /api/v1/users/{userId}/wallet
     * The caller's JWT is forwarded to Wallet Service.
     */
    private WalletView getOwnWallet(Long userId, String accessToken) {

        try {

            return walletRestClient.get()

                    // Replace {userId} with the actual customer ID.
                    .uri("/api/v1/users/{userId}/wallet", userId)

                    // Forward the caller's Bearer token.
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            bearer(accessToken)
                    )

                    // Execute the request and process HTTP errors.
                    .retrieve()

                    // Convert the JSON response into WalletView.
                    .body(WalletView.class);

        } catch (RestClientResponseException ex) {

            // A missing wallet is represented as a known business rejection rather than a generic server failure.
            if (ex.getStatusCode().value() == 404) {
                throw new WalletDeclineException("WALLET_NOT_FOUND");
            }

            // Other HTTP errors are handled by the calling method. Other HTTP errors are handled by the calling method.
            throw ex;
        }
    }

    /**
     * Withdraws money from a wallet.
     * POST /api/v1/wallets/{walletId}/withdrawals
     * This is the actual debit operation.
     */
    private TransactionView withdraw(
            Long walletId,
            BigDecimal amount,
            String idempotencyKey,
            String accessToken
    ) {

        try {

            return walletRestClient.post()

                    // Select the wallet that should be debited.
                    .uri(
                            "/api/v1/wallets/{walletId}/withdrawals",
                            walletId
                    )

                    // Authenticate the request.
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            bearer(accessToken)
                    )

                    // Protect against duplicate withdrawals on retries.
                    .header("Idempotency-Key", idempotencyKey)

                    // Tell Wallet Service that the body is JSON.
                    .contentType(MediaType.APPLICATION_JSON)

                    // Send the withdrawal amount.
                    .body(Map.of("amount", amount))

                    // Execute the HTTP request.
                    .retrieve()

                    // Convert the response into a transaction object.
                    .body(TransactionView.class);

        } catch (RestClientResponseException ex) {

            // The current Wallet Service contract uses 422 for a withdrawal business rejection.
            if (ex.getStatusCode().value() == 422) {
                throw new WalletDeclineException(readCode(ex));
            }

            // Let the outer charge method classify other HTTP errors. Let the outer charge method classify other HTTP errors.
            throw ex;
        }
    }

    /**
     * Deposits money into a wallet.
     * POST /api/v1/wallets/{walletId}/deposits
     * This is used for refunds in the current project.
     */
    private TransactionView deposit(
            Long walletId,
            BigDecimal amount,
            String idempotencyKey,
            String accessToken
    ) {

        return walletRestClient.post()

                // Select the wallet receiving the refund.
                .uri(
                        "/api/v1/wallets/{walletId}/deposits",
                        walletId
                )

                // Authenticate the request.
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(accessToken)
                )

                // Prevent duplicate deposits when retrying a refund.
                .header("Idempotency-Key", idempotencyKey)

                // Send JSON.
                .contentType(MediaType.APPLICATION_JSON)

                // Send the amount to deposit.
                .body(Map.of("amount", amount))

                // Execute the request and convert the response.
                .retrieve()
                .body(TransactionView.class);
    }

    /**
     * Extracts a machine-readable error code from the HTTP response body.
     * Expected example: { "code": "INSUFFICIENT_BALANCE" } If the response body is missing or invalid JSON, return a safe fallback error code.
     */
    private String readCode(RestClientResponseException ex) {

        try {

            // Parse the error response body as JSON.
            JsonNode node = objectMapper.readTree(
                    ex.getResponseBodyAsString()
            );

            // Extract the "code" field if present and non-null.
            if (node.hasNonNull("code")) {
                return node.get("code").asText();
            }

        } catch (Exception ignored) {

            // If JSON parsing fails, use the fallback below.
            // Do not expose raw response bodies to callers or logs.
        }

        return "WALLET_REJECTED";
    }

    /**
     * Validates that an access token is available.
     * A missing token must not result in an unauthenticated wallet request.
     */
    private static void requireToken(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new WalletDeclineException("MISSING_ACCESS_TOKEN");
        }
    }

    /**
     * Ensures the Authorization header has the Bearer prefix.
     * Accepts either: - eyJhbGciOi... - Bearer eyJhbGciOi...
     */
    private static String bearer(String accessToken) {

        return accessToken.startsWith("Bearer ")
                ? accessToken
                : "Bearer " + accessToken;
    }

    /**
     * Minimal representation of a wallet API response.
     * We only model the fields Commerce Service needs.
     * This is not a JPA entity. It is a response-mapping record.
     */
    public record WalletView(
            Long walletId,
            Long userId,
            String currency
    ) {
    }

    /**
     * Minimal representation of a wallet transaction response.
     */
    public record TransactionView(
            Long id,
            Long walletId
    ) {
    }

    /**
     * Internal exception for known wallet business rejections.
     * This exception is private to this adapter.
     * The public contract remains PaymentResult.
     */
    private static final class WalletDeclineException extends RuntimeException {
        private final String code;
        private WalletDeclineException(String code) { super(code); this.code = code; }
        private String code() { return code; }
    }
}