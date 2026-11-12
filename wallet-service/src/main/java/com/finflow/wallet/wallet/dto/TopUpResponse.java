package com.finflow.wallet.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Successful wallet top-up after payment provider capture.
 */
public record TopUpResponse(
        Long transactionId,
        Long walletId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String paymentProviderReference,
        Boolean replayed,
        Instant createdAt
) {
}
