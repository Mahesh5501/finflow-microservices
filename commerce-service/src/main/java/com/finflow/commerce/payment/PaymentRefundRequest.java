package com.finflow.commerce.payment;

import java.math.BigDecimal;

/**
 * Immutable request data required to refund a customer's wallet.
 *
 * This request is sent through PaymentPort.refund().
 *
 * It contains the original wallet ID when known, so the refund can be
 * credited to the wallet involved in the original payment.
 */
public record PaymentRefundRequest(

        // Identifies the customer receiving the refund.
        Long customerUserId,

        // Wallet that should receive the refund.
        // It may be null when the adapter needs to resolve the customer's wallet.
        Long walletId,

        // Amount to credit back to the wallet.
        BigDecimal amount,

        // Stable key to prevent duplicate refunds when the operation is retried.
        String idempotencyKey,

        // Caller authentication token forwarded to wallet-service.
        // Never include this value in application logs.
        String accessToken

) {
}