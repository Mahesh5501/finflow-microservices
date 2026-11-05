package com.finflow.transaction.event;

import com.finflow.transaction.entity.TransactionStatus;
import com.finflow.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDomainEvent(
        String eventId,
        Long transactionId,
        TransactionType type,
        TransactionStatus status,
        Long actorUserId,
        Long sourceWalletId,
        Long destinationWalletId,
        BigDecimal amount,
        String currencyCode,
        Instant occurredAt
) {
}
