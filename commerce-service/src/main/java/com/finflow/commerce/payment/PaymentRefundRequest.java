package com.finflow.commerce.payment;

import java.math.BigDecimal;

/**
 * Immutable request data required to refund a customer's wallet.
 * A Java record automatically provides a constructor containing all components, accessor methods such as customerUserId() and amount(), equals() and hashCode(), and toString().
 * @param customerUserId the customer receiving the refund.
 * @param walletId the wallet that should receive the refund. It may be null when the adapter needs to resolve the customer's wallet.
 * @param amount the amount to credit back to the wallet. BigDecimal avoids floating-point money errors.
 * @param idempotencyKey the stable key to prevent duplicate refunds when the operation is retried.
 * @param accessToken the caller authentication token forwarded to wallet-service. Treat this as sensitive data: do not log it.
 */
public record PaymentRefundRequest(
        Long customerUserId,
        Long walletId,
        BigDecimal amount,
        String idempotencyKey,
        String accessToken
) {
}