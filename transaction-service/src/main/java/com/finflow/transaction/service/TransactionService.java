package com.finflow.transaction.service;

import com.finflow.transaction.dto.TransactionResponse;
import com.finflow.transaction.dto.LedgerEntryResponse;
import com.finflow.transaction.dto.LedgerBalanceResponse;
import com.finflow.transaction.dto.MoneyRequest;
import com.finflow.transaction.dto.TransferRequest;
import com.finflow.transaction.entity.LedgerTransaction;
import com.finflow.transaction.entity.LedgerEntry;
import com.finflow.transaction.entity.LedgerEntryType;
import com.finflow.transaction.entity.TransactionStatus;
import com.finflow.transaction.entity.TransactionStatusHistory;
import com.finflow.transaction.entity.TransactionType;
import com.finflow.transaction.event.TransactionDomainEvent;
import com.finflow.transaction.event.TransactionEventPublisher;
import com.finflow.transaction.exception.ApiException;
import com.finflow.transaction.repository.LedgerTransactionRepository;
import com.finflow.transaction.repository.LedgerEntryRepository;
import com.finflow.transaction.repository.TransactionStatusHistoryRepository;
import com.finflow.transaction.wallet.WalletClientException;
import com.finflow.transaction.wallet.WalletClient;
import com.finflow.transaction.wallet.WalletAccount;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates money moves: local PENDING row → wallet HTTP → ledger post → SUCCESS/FAILED/REVERSED.
 * Wallet HTTP stays outside the local DB transaction (no long locks across the network).
 */
@Service
public class TransactionService {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionStatusHistoryRepository statusHistoryRepository;
    private final WalletClient walletClient;
    private final TransactionEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public TransactionService(
            LedgerTransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            TransactionStatusHistoryRepository statusHistoryRepository,
            WalletClient walletClient,
            TransactionEventPublisher eventPublisher,
            TransactionTemplate transactionTemplate
    ) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.walletClient = walletClient;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    public TransactionResponse deposit(
            Long actorUserId,
            String idempotencyKey,
            String authorization,
            MoneyRequest request
    ) {
        String key = requireKey(idempotencyKey);
        LedgerTransaction existing = findByKey(key);
        if (existing != null) {
            return toResponse(existing, true);
        }

        WalletAccount wallet = walletClient.getWallet(request.walletId(), authorization);
        requireOwner(wallet, actorUserId);
        BigDecimal amount = normalize(request.amount());

        LedgerTransaction tx = begin(
                key,
                actorUserId,
                TransactionType.DEPOSIT,
                request.walletId(),
                null,
                amount,
                wallet.currency()
        );
        if (tx.getStatus() != TransactionStatus.PENDING) {
            return toResponse(tx, true);
        }

        try {
            walletClient.deposit(request.walletId(), amount, key + ":wallet", authorization);
            completeSuccess(tx, List.of(
                    credit(tx.getTransactionId(), request.walletId(), amount)
            ));
        } catch (WalletClientException ex) {
            completeFailed(tx, ex.getCode());
        }

        return toResponse(require(tx.getTransactionId()), false);
    }

    public TransactionResponse withdraw(
            Long actorUserId,
            String idempotencyKey,
            String authorization,
            MoneyRequest request
    ) {
        String key = requireKey(idempotencyKey);
        LedgerTransaction existing = findByKey(key);
        if (existing != null) {
            return toResponse(existing, true);
        }

        WalletAccount wallet = walletClient.getWallet(request.walletId(), authorization);
        requireOwner(wallet, actorUserId);
        BigDecimal amount = normalize(request.amount());

        LedgerTransaction tx = begin(
                key,
                actorUserId,
                TransactionType.WITHDRAWAL,
                request.walletId(),
                null,
                amount,
                wallet.currency()
        );
        if (tx.getStatus() != TransactionStatus.PENDING) {
            return toResponse(tx, true);
        }

        try {
            walletClient.withdraw(request.walletId(), amount, key + ":wallet", authorization);
            completeSuccess(tx, List.of(
                    debit(tx.getTransactionId(), request.walletId(), amount)
            ));
        } catch (WalletClientException ex) {
            completeFailed(tx, ex.getCode());
        }

        return toResponse(require(tx.getTransactionId()), false);
    }

    /**
     * Transfer saga: withdraw source → deposit destination.
     * If credit fails after debit, compensate by depositing back to source → REVERSED.
     */
    public TransactionResponse transfer(
            Long actorUserId,
            String idempotencyKey,
            String authorization,
            TransferRequest request
    ) {
        String key = requireKey(idempotencyKey);
        LedgerTransaction existing = findByKey(key);
        if (existing != null) {
            return toResponse(existing, true);
        }
        if (request.sourceWalletId().equals(request.destinationWalletId())) {
            throw ApiException.badRequest("Source and destination wallet must differ");
        }

        WalletAccount source = walletClient.getWallet(request.sourceWalletId(), authorization);
        requireOwner(source, actorUserId);
        WalletAccount dest = walletClient.getWallet(request.destinationWalletId(), authorization);
        if (!source.currency().equals(dest.currency())) {
            throw ApiException.unprocessable("CURRENCY_MISMATCH", "Wallets must share the same currency");
        }
        BigDecimal amount = normalize(request.amount());

        LedgerTransaction tx = begin(
                key,
                actorUserId,
                TransactionType.TRANSFER,
                request.sourceWalletId(),
                request.destinationWalletId(),
                amount,
                source.currency()
        );
        if (tx.getStatus() != TransactionStatus.PENDING) {
            return toResponse(tx, true);
        }

        try {
            walletClient.withdraw(request.sourceWalletId(), amount, key + ":debit", authorization);
        } catch (WalletClientException ex) {
            completeFailed(tx, ex.getCode());
            return toResponse(require(tx.getTransactionId()), false);
        }

        try {
            walletClient.deposit(request.destinationWalletId(), amount, key + ":credit", authorization);
            completeSuccess(tx, List.of(
                    debit(tx.getTransactionId(), request.sourceWalletId(), amount),
                    credit(tx.getTransactionId(), request.destinationWalletId(), amount)
            ));
        } catch (RuntimeException creditFailure) {
            try {
                walletClient.deposit(request.sourceWalletId(), amount, key + ":compensate", authorization);
            } catch (RuntimeException compensateFailure) {
                completeFailed(tx, "COMPENSATION_FAILED");
                throw ApiException.conflict(
                        "COMPENSATION_FAILED",
                        "Debit succeeded but credit and compensation both failed. Manual reconcile required. tx="
                                + tx.getTransactionId()
                );
            }
            String code = creditFailure instanceof WalletClientException w
                    ? w.getCode()
                    : "CREDIT_FAILED";
            completeReversed(tx, code, List.of(
                    debit(tx.getTransactionId(), request.sourceWalletId(), amount),
                    credit(tx.getTransactionId(), request.sourceWalletId(), amount)
            ));
        }

        return toResponse(require(tx.getTransactionId()), false);
    }

    public TransactionResponse get(Long transactionId, Long actorUserId) {
        LedgerTransaction tx = require(transactionId);
        requireActorOrParty(tx, actorUserId);
        return toResponse(tx, null);
    }

    public List<TransactionResponse> history(Long walletId, Long actorUserId, String authorization) {
        WalletAccount wallet = walletClient.getWallet(walletId, authorization);
        requireOwner(wallet, actorUserId);
        return transactionRepository
                .findBySourceWalletIdOrDestinationWalletIdOrderByCreatedAtDesc(walletId, walletId)
                .stream()
                .map(tx -> toResponse(tx, null))
                .toList();
    }

    public LedgerBalanceResponse ledgerBalance(Long walletId, Long actorUserId, String authorization) {
        WalletAccount wallet = walletClient.getWallet(walletId, authorization);
        requireOwner(wallet, actorUserId);
        BigDecimal sum = ledgerEntryRepository.sumSignedAmountByWalletId(walletId);
        return new LedgerBalanceResponse(walletId, sum == null ? BigDecimal.ZERO.setScale(2) : sum);
    }

    private LedgerTransaction begin(
            String key,
            Long actorUserId,
            TransactionType type,
            Long sourceWalletId,
            Long destinationWalletId,
            BigDecimal amount,
            String currency
    ) {
        try {
            return transactionTemplate.execute(status -> {
                LedgerTransaction tx = new LedgerTransaction(
                        key, actorUserId, type, sourceWalletId, destinationWalletId, amount, currency
                );
                tx = transactionRepository.saveAndFlush(tx);
                statusHistoryRepository.save(new TransactionStatusHistory(
                        tx.getTransactionId(), null, TransactionStatus.PENDING
                ));
                return tx;
            });
        } catch (DataIntegrityViolationException ex) {
            LedgerTransaction winner = findByKey(key);
            if (winner != null) {
                return winner;
            }
            throw ex;
        }
    }

    private void completeSuccess(LedgerTransaction tx, List<LedgerEntry> entries) {
        transactionTemplate.executeWithoutResult(status -> {
            LedgerTransaction managed = require(tx.getTransactionId());
            if (managed.getStatus() != TransactionStatus.PENDING) {
                return;
            }
            ledgerEntryRepository.saveAll(entries);
            TransactionStatus from = managed.getStatus();
            managed.markSuccess();
            transactionRepository.save(managed);
            statusHistoryRepository.save(new TransactionStatusHistory(
                    managed.getTransactionId(), from, TransactionStatus.SUCCESS
            ));
            eventPublisher.publish(toEvent(managed));
        });
    }

    private void completeFailed(LedgerTransaction tx, String failureCode) {
        transactionTemplate.executeWithoutResult(status -> {
            LedgerTransaction managed = require(tx.getTransactionId());
            if (managed.getStatus() != TransactionStatus.PENDING) {
                return;
            }
            TransactionStatus from = managed.getStatus();
            managed.markFailed(failureCode);
            transactionRepository.save(managed);
            statusHistoryRepository.save(new TransactionStatusHistory(
                    managed.getTransactionId(), from, TransactionStatus.FAILED
            ));
            eventPublisher.publish(toEvent(managed));
        });
    }

    private void completeReversed(LedgerTransaction tx, String failureCode, List<LedgerEntry> entries) {
        transactionTemplate.executeWithoutResult(status -> {
            LedgerTransaction managed = require(tx.getTransactionId());
            if (managed.getStatus() != TransactionStatus.PENDING) {
                return;
            }
            ledgerEntryRepository.saveAll(entries);
            TransactionStatus from = managed.getStatus();
            managed.markReversed(failureCode);
            transactionRepository.save(managed);
            statusHistoryRepository.save(new TransactionStatusHistory(
                    managed.getTransactionId(), from, TransactionStatus.REVERSED
            ));
            eventPublisher.publish(toEvent(managed));
        });
    }

    private TransactionResponse toResponse(LedgerTransaction tx, Boolean replayed) {
        List<LedgerEntryResponse> entries = ledgerEntryRepository
                .findByTransactionIdOrderByPostedAtAsc(tx.getTransactionId())
                .stream()
                .map(LedgerEntryResponse::from)
                .toList();
        return TransactionResponse.from(tx, entries, replayed);
    }

    private LedgerTransaction findByKey(String key) {
        return transactionRepository.findByIdempotencyKey(key).orElse(null);
    }

    private LedgerTransaction require(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("TRANSACTION_NOT_FOUND", "Transaction not found: " + id));
    }

    private static void requireOwner(WalletAccount wallet, Long actorUserId) {
        if (!wallet.userId().equals(actorUserId)) {
            throw ApiException.forbidden("Not your wallet");
        }
    }

    private static void requireActorOrParty(LedgerTransaction tx, Long actorUserId) {
        if (!tx.getActorUserId().equals(actorUserId)) {
            throw ApiException.forbidden("Not a party to this transaction");
        }
    }

    private static String requireKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        return idempotencyKey.trim();
    }

    private static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static LedgerEntry credit(Long txId, Long walletId, BigDecimal amount) {
        return new LedgerEntry(txId, walletId, amount, LedgerEntryType.CREDIT);
    }

    private static LedgerEntry debit(Long txId, Long walletId, BigDecimal amount) {
        return new LedgerEntry(txId, walletId, amount.negate(), LedgerEntryType.DEBIT);
    }

    private static TransactionDomainEvent toEvent(LedgerTransaction tx) {
        return new TransactionDomainEvent(
                UUID.randomUUID().toString(),
                tx.getTransactionId(),
                tx.getType(),
                tx.getStatus(),
                tx.getActorUserId(),
                tx.getSourceWalletId(),
                tx.getDestinationWalletId(),
                tx.getAmount(),
                tx.getCurrencyCode(),
                Instant.now()
        );
    }
}
