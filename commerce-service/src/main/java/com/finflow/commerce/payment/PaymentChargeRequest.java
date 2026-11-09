package com.finflow.commerce.payment;

import java.math.BigDecimal;

/**
 * Immutable request data required to charge a customer's wallet.
 *
 * A Java record automatically provides:
 * - A constructor containing all components.
 * - Accessor methods such as customerUserId() and amount().
 * - equals() and hashCode().
 * - toString().
 *
 * The request is passed from the payment settlement service
 * to the selected PaymentPort implementation.
 */
public record PaymentChargeRequest(

        // Identifies the customer whose wallet must be charged.
        Long customerUserId,

        // Amount to debit. BigDecimal avoids floating-point money errors.
        BigDecimal amount,

        // Currency of the payment, for example INR or USD.
        String currencyCode,

        // Stable key used to prevent duplicate charges when retrying.
        String idempotencyKey,

        // Caller authentication token forwarded to wallet-service.
        // Treat this as sensitive data: do not log it.
        String accessToken,

        // Optional outcome used only by the simulated payment implementation.
        // It lets local tests/demo flows simulate APPROVED, DECLINED or UNCERTAIN.
        PaymentOutcome sandboxHint

) {
}