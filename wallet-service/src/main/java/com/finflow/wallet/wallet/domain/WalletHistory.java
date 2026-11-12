package com.finflow.wallet.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One statement / activity line for a wallet (market term: transaction history).
 *
 * <h2>Why a history table?</h2>
 * {@code wallet.balance} is a fast cache. History is the audit trail and supports
 * {@code balance_after} so UIs do not recompute running totals.
 *
 * <h2>Row shapes</h2>
 * <ul>
 *   <li>DEPOSIT / WITHDRAW — {@code idempotencyKey} set; from/to null</li>
 *   <li>SENT / RECEIVED — {@code transferId} + {@code fromWalletId}/{@code toWalletId} set</li>
 * </ul>
 *
 * <h2>DB optimisation</h2>
 * Unique {@code idempotency_key} (NULLs allowed for transfer legs) enables O(1) retry lookup
 * and prevents double deposit/withdraw under concurrency.
 */
@Entity
@Table(
        name = "wallet_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_history_idempotency_key", columnNames = "idempotency_key")
        }
)
public class WalletHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    /** Wallet this statement line belongs to. */
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Snapshot of wallet.balance right after this line posted. */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    /** Transfer sender (null for deposit/withdraw). */
    @Column(name = "from_wallet_id")
    private Long fromWalletId;

    /** Transfer receiver (null for deposit/withdraw). */
    @Column(name = "to_wallet_id")
    private Long toWalletId;

    /** FK-style link to wallet_transfer for SENT/RECEIVED. */
    @Column(name = "transfer_id")
    private Long transferId;

    /** Deposit/withdraw retry key only; null on transfer legs. */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletHistory() {
    }

    public WalletHistory(
            Long walletId,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Long fromWalletId,
            Long toWalletId,
            Long transferId,
            String idempotencyKey
    ) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.transferId = transferId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Long getFromWalletId() {
        return fromWalletId;
    }

    public Long getToWalletId() {
        return toWalletId;
    }

    public Long getTransferId() {
        return transferId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
