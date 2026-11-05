package com.finflow.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable posted ledger line. Never edit — reverse with a new row.
 * For TRANSFER, debit + credit signed amounts must net to zero.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long entryId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    /** Signed amount: credit positive, debit negative. */
    @Column(name = "signed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal signedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerEntryType entryType;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(Long transactionId, Long walletId, BigDecimal signedAmount, LedgerEntryType entryType) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.signedAmount = signedAmount;
        this.entryType = entryType;
        this.postedAt = Instant.now();
    }

    public Long getEntryId() {
        return entryId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getWalletId() {
        return walletId;
    }

    public BigDecimal getSignedAmount() {
        return signedAmount;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public Instant getPostedAt() {
        return postedAt;
    }
}
