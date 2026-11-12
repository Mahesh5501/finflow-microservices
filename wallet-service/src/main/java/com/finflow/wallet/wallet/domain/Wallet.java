package com.finflow.wallet.wallet.domain;

import com.finflow.wallet.exception.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Aggregate root — current balance and status for one user.
 *
 * <h2>Interview: why domain methods?</h2>
 * {@link #credit} / {@link #debit} keep invariants next to the data (active status,
 * positive amount, sufficient funds). Service orchestrates; entity enforces rules.
 *
 * <h2>Concurrency</h2>
 * {@link Version} optimistic lock: UPDATE ... WHERE version = ?. Stale writer → 409.
 * Good for moderate contention; ultra-hot wallets may need queues or SELECT FOR UPDATE.
 *
 * <h2>Money type</h2>
 * Always {@link BigDecimal} with scale 2 — never {@code double}/{@code float}.
 */
@Entity
@Table(
        name = "wallet",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_user_id", columnNames = "user_id")
        }
)
public class Wallet {

    /** Fixed minor units (paise/cents). */
    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    /** Business owner of this wallet (unique). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Materialised balance — O(1) GET; must stay consistent with history writes. */
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status;

    /**
     * Optimistic lock token. Hibernate bumps it on each successful UPDATE.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Wallet() {
        // JPA
    }

    public Wallet(Long userId, String currency) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        this.currency = currency;
        this.status = WalletStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    /** Increase balance (deposit / transfer-in). Rejects BLOCKED/CLOSED. */
    public void credit(BigDecimal amount) {
        requireActive();
        requirePositiveAmount(amount);
        this.balance = this.balance.add(normalize(amount));
    }

    /**
     * Decrease balance (withdraw / transfer-out).
     * Check-then-act is safe together with {@link Version} at flush.
     */
    public void debit(BigDecimal amount) {
        requireActive();
        requirePositiveAmount(amount);
        BigDecimal normalized = normalize(amount);
        if (this.balance.compareTo(normalized) < 0) {
            throw ApiException.unprocessable(
                    "INSUFFICIENT_BALANCE",
                    "Insufficient balance. available=" + this.balance + ", requested=" + normalized
            );
        }
        this.balance = this.balance.subtract(normalized);
    }

    private void requireActive() {
        if (status != WalletStatus.ACTIVE) {
            throw ApiException.conflict("WALLET_NOT_ACTIVE", "Wallet is not ACTIVE: status=" + status);
        }
    }

    /** Risk / admin hold. */
    public void block() {
        if (status == WalletStatus.CLOSED) {
            throw ApiException.conflict("WALLET_NOT_ACTIVE", "Closed wallet cannot be blocked");
        }
        if (status == WalletStatus.BLOCKED) {
            return;
        }
        this.status = WalletStatus.BLOCKED;
    }

    public void unblock() {
        if (status == WalletStatus.CLOSED) {
            throw ApiException.conflict("WALLET_NOT_ACTIVE", "Closed wallet cannot be unblocked");
        }
        if (status == WalletStatus.ACTIVE) {
            return;
        }
        this.status = WalletStatus.ACTIVE;
    }

    /** Permanent close — balance must be zero. */
    public void close() {
        if (status == WalletStatus.CLOSED) {
            return;
        }
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Wallet balance must be zero before close");
        }
        this.status = WalletStatus.CLOSED;
    }

    private static void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
