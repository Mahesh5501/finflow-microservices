package com.finflow.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finflow.wallet.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire model for one history / money-move line.
 *
 * <p>{@code replayed=true} means this response is an idempotent replay (no new money move).
 * Null fields are omitted in JSON (deposit/withdraw hide from/to/transferId).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        Long id,
        Long walletId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Long fromWalletId,
        Long toWalletId,
        Long transferId,
        Boolean replayed,
        Instant createdAt
) {
}
