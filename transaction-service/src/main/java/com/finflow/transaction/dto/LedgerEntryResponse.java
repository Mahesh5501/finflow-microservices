package com.finflow.transaction.dto;

import com.finflow.transaction.entity.LedgerEntry;
import com.finflow.transaction.entity.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        Long entryId,
        Long walletId,
        BigDecimal signedAmount,
        LedgerEntryType entryType,
        Instant postedAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getEntryId(),
                entry.getWalletId(),
                entry.getSignedAmount(),
                entry.getEntryType(),
                entry.getPostedAt()
        );
    }
}
