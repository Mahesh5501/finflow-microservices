package com.finflow.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finflow.transaction.entity.LedgerTransaction;
import com.finflow.transaction.entity.TransactionStatus;
import com.finflow.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        Long transactionId,
        TransactionType type,
        TransactionStatus status,
        Long sourceWalletId,
        Long destinationWalletId,
        BigDecimal amount,
        String currencyCode,
        String failureCode,
        Boolean replayed,
        Instant createdAt,
        List<LedgerEntryResponse> ledgerEntries
) {
    public static TransactionResponse from(
            LedgerTransaction tx,
            List<LedgerEntryResponse> entries,
            Boolean replayed
    ) {
        return new TransactionResponse(
                tx.getTransactionId(),
                tx.getType(),
                tx.getStatus(),
                tx.getSourceWalletId(),
                tx.getDestinationWalletId(),
                tx.getAmount(),
                tx.getCurrencyCode(),
                tx.getFailureCode(),
                replayed,
                tx.getCreatedAt(),
                entries
        );
    }
}
