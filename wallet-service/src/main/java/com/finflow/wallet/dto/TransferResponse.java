package com.finflow.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/** Result of a wallet-to-wallet transfer. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferResponse(
        Long transferId,
        Long fromWalletId,
        Long toWalletId,
        BigDecimal amount,
        BigDecimal fromBalanceAfter,
        BigDecimal toBalanceAfter,
        Boolean replayed,
        Instant createdAt
) {
}
