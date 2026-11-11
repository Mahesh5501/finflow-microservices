package com.finflow.commerce.payment;

import java.math.BigDecimal;

/**
 * Immutable request data required to charge a customer's wallet.
 * A Java record automatically provides a constructor containing all components, accessor methods such as customerUserId() and amount(), equals() and hashCode(), and toString().
 * The request is passed from the payment settlement service to the selected PaymentGateway implementation.
 */
public record PaymentChargeRequest(
        Long customerUserId, // Identifies the customer whose wallet must be charged.
        BigDecimal amount, // Amount to debit. BigDecimal avoids floating-point money errors.
        String currencyCode, // Currency of the payment, for example INR or USD.
        String idempotencyKey, // Stable key used to prevent duplicate charges when retrying.
        String accessToken, // Caller authentication token forwarded to wallet-service. Treat this as sensitive data: do not log it.
        PaymentOutcome sandboxHint // Optional outcome used only by the simulated payment implementation. It lets local tests/demo flows simulate APPROVED, DECLINED or UNCERTAIN.
) {
}