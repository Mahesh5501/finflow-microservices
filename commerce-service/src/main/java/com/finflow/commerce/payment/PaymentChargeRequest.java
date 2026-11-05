package com.finflow.commerce.payment;

import java.math.BigDecimal;

/** Debit customer wallet (or sandbox) for an order. */
public record PaymentChargeRequest(
        Long customerUserId,
        BigDecimal amount,
        String currencyCode,
        String idempotencyKey,
        String accessToken,
        /** Used only by {@link SimulatedPaymentAdapter}. */
        PaymentOutcome sandboxHint
) {
}
