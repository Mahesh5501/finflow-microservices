package com.finflow.transaction.entity;

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

@Entity
@Table(
        name = "ledger_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ledger_tx_idempotency_key",
                columnNames = "idempotency_key"
        )
)
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "source_wallet_id")
    private Long sourceWalletId;

    @Column(name = "destination_wallet_id")
    private Long destinationWalletId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "failure_code", length = 60)
    private String failureCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(
            String idempotencyKey,
            Long actorUserId,
            TransactionType type,
            Long sourceWalletId,
            Long destinationWalletId,
            BigDecimal amount,
            String currencyCode
    ) {
        this.idempotencyKey = idempotencyKey;
        this.actorUserId = actorUserId;
        this.type = type;
        this.status = TransactionStatus.PENDING;
        this.sourceWalletId = sourceWalletId;
        this.destinationWalletId = destinationWalletId;
        this.amount = amount;
        this.currencyCode = currencyCode;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markSuccess() {
        this.status = TransactionStatus.SUCCESS;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String failureCode) {
        this.status = TransactionStatus.FAILED;
        this.failureCode = failureCode;
        this.updatedAt = Instant.now();
    }

    public void markReversed(String failureCode) {
        this.status = TransactionStatus.REVERSED;
        this.failureCode = failureCode;
        this.updatedAt = Instant.now();
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Long getSourceWalletId() {
        return sourceWalletId;
    }

    public Long getDestinationWalletId() {
        return destinationWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
