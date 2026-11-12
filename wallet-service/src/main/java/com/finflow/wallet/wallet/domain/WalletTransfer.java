package com.finflow.wallet.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One P2P transfer record: money moved {@code from_wallet_id} → {@code to_wallet_id}.
 *
 * <h2>Why a separate transfer table? (interview)</h2>
 * Fintech systems keep the <b>business transfer</b> distinct from per-wallet history legs.
 * One row answers “who paid whom?”, carries one idempotency key for the whole move,
 * and stores both balance snapshots after the transfer.
 *
 * <p>History still gets SENT + RECEIVED lines for each wallet’s statement.
 */
@Entity
@Table(
        name = "wallet_transfer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_transfer_idempotency_key", columnNames = "idempotency_key")
        }
)
public class WalletTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long id;

    /** Client retry key for the whole A→B move (not per leg). */
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "from_wallet_id", nullable = false)
    private Long fromWalletId;

    @Column(name = "to_wallet_id", nullable = false)
    private Long toWalletId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Sender balance after this transfer. */
    @Column(name = "from_balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal fromBalanceAfter;

    /** Receiver balance after this transfer. */
    @Column(name = "to_balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal toBalanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletTransfer() {
    }

    public WalletTransfer(
            String idempotencyKey,
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            BigDecimal fromBalanceAfter,
            BigDecimal toBalanceAfter
    ) {
        this.idempotencyKey = idempotencyKey;
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.fromBalanceAfter = fromBalanceAfter;
        this.toBalanceAfter = toBalanceAfter;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getFromWalletId() {
        return fromWalletId;
    }

    public Long getToWalletId() {
        return toWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFromBalanceAfter() {
        return fromBalanceAfter;
    }

    public BigDecimal getToBalanceAfter() {
        return toBalanceAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
