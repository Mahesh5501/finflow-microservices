package com.finflow.wallet.wallet.dto;

import com.finflow.wallet.wallet.domain.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Stable API view of a wallet. Safe to evolve with additive fields. */
public record WalletResponse(
        Long walletId,
        Long userId,
        BigDecimal balance,
        String currency,
        WalletStatus status,
        Instant createdAt
) {
}
