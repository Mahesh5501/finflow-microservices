package com.finflow.commerce.payment;

import java.math.BigDecimal;

/** Credit customer wallet after a confirmed order is cancelled. */
public record PaymentRefundRequest(
        Long customerUserId,
        Long walletId,
        BigDecimal amount,
        String idempotencyKey,
        String accessToken
) {
}
