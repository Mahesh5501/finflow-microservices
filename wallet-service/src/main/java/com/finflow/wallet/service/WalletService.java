package com.finflow.wallet.service;

import com.finflow.wallet.dto.TransactionResponse;
import com.finflow.wallet.dto.TransferResponse;
import com.finflow.wallet.dto.WalletResponse;
import com.finflow.wallet.entity.TransactionType;
import com.finflow.wallet.entity.Wallet;
import com.finflow.wallet.entity.WalletHistory;
import com.finflow.wallet.entity.WalletTransfer;
import com.finflow.wallet.exception.ApiException;
import com.finflow.wallet.repository.WalletHistoryRepository;
import com.finflow.wallet.repository.WalletRepository;
import com.finflow.wallet.repository.WalletTransferRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application service — all wallet use cases and transaction boundaries.
 *
 * <h2>Tables</h2>
 * {@code wallet} (balance) · {@code wallet_history} (statement + deposit/withdraw idempotency) ·
 * {@code wallet_transfer} (P2P from→to + transfer idempotency).
 *
 * <h2>Interview topics covered here</h2>
 * <ul>
 *   <li><b>Idempotency</b> — lookup by key first; unique constraint closes races</li>
 *   <li><b>Atomic transfer</b> — debit + credit + history in one {@code @Transactional}</li>
 *   <li><b>Optimistic locking</b> — {@code Wallet.version} → 409 on concurrent writers</li>
 *   <li><b>Race on create/idempotent write</b> — catch {@link DataIntegrityViolationException}</li>
 * </ul>
 *
 * <h2>Heavy traffic honesty</h2>
 * Correct under moderate load. Hot wallets may see many 409s (client should retry with same key).
 * Scale-up later: pagination, replicas, Redis cache with {@code wallet:} key prefix
 * (shared Docker Redis — no Redis microservice; extract finflow-common only if we duplicate
 * identity's Redis+fallback helper), queue for hot accounts.
 */
@Service
public class WalletService {

    /** Balance + status store. */
    private final WalletRepository walletRepository;

    /** P2P transfer store (from → to). */
    private final WalletTransferRepository walletTransferRepository;

    /** Statement / activity store. */
    private final WalletHistoryRepository walletHistoryRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletTransferRepository walletTransferRepository,
            WalletHistoryRepository walletHistoryRepository
    ) {
        this.walletRepository = walletRepository;
        this.walletTransferRepository = walletTransferRepository;
        this.walletHistoryRepository = walletHistoryRepository;
    }

    /**
     * Creates a zero-balance wallet.
     * Pre-check + unique {@code user_id} handle double-submit races → 409.
     */
    @Transactional
    public WalletResponse createWallet(Long userId, String currency) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }

        if (walletRepository.existsByUserId(userId)) {
            throw ApiException.conflict("WALLET_ALREADY_EXISTS", "Wallet already exists for this user");
        }

        try {
            Wallet wallet = new Wallet(userId, currency.trim().toUpperCase());
            return toWalletResponse(walletRepository.save(wallet));
        } catch (DataIntegrityViolationException ex) {
            // Two creates raced past existsByUserId; DB unique wins.
            throw ApiException.conflict("WALLET_ALREADY_EXISTS", "Wallet already exists for this user");
        }
    }

    /** Point read of balance (O(1) — materialised column, not SUM of history). */
    @Transactional(readOnly = true)
    public WalletResponse getWalletById(Long walletId, Long callerUserId) {
        return toWalletResponse(requireOwnedWallet(walletId, callerUserId));
    }

    /** Lookup by user when client does not have walletId yet (self only). */
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(Long userId, Long callerUserId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (!userId.equals(callerUserId)) {
            throw ApiException.forbidden("Cannot read another user's wallet");
        }
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("WALLET_NOT_FOUND", "Wallet not found for user: " + userId));
        return toWalletResponse(wallet);
    }

    /** Read a past P2P transfer — caller must own from or to wallet. */
    @Transactional(readOnly = true)
    public TransferResponse getTransfer(Long transferId, Long callerUserId) {
        if (transferId == null || transferId <= 0) {
            throw new IllegalArgumentException("Transfer ID must be positive");
        }
        WalletTransfer transfer = walletTransferRepository.findById(transferId)
                .orElseThrow(() -> ApiException.notFound("TRANSFER_NOT_FOUND", "Transfer not found: " + transferId));
        requirePartyToTransfer(transfer, callerUserId);
        return toTransferResponse(transfer, null);
    }

    /** Add money. Idempotent via history.idempotency_key. */
    @Transactional
    public TransactionResponse deposit(Long walletId, Long callerUserId, BigDecimal amount, String idempotencyKey) {
        requireOwnedWallet(walletId, callerUserId);
        return applyDepositOrWithdraw(walletId, amount, idempotencyKey, TransactionType.DEPOSIT);
    }

    /** Remove money. Idempotent; 422 if insufficient balance. */
    @Transactional
    public TransactionResponse withdraw(Long walletId, Long callerUserId, BigDecimal amount, String idempotencyKey) {
        requireOwnedWallet(walletId, callerUserId);
        return applyDepositOrWithdraw(walletId, amount, idempotencyKey, TransactionType.WITHDRAW);
    }

    /**
     * Move money A → B atomically.
     * <ol>
     *   <li>Replay if idempotency key already used</li>
     *   <li>Debit A, credit B (same currency, both ACTIVE)</li>
     *   <li>Insert {@code wallet_transfer}</li>
     *   <li>Insert SENT + RECEIVED history (with from/to wallet ids)</li>
     * </ol>
     * On unique-key race: mark TX rollback-only and return the winning transfer.
     */
    @Transactional
    public TransferResponse transfer(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String idempotencyKey,
            Long callerUserId
    ) {
        requireIdempotencyKey(idempotencyKey);
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("From and to wallet must be different");
        }

        var existing = walletTransferRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existing.isPresent()) {
            WalletTransfer replay = existing.get();
            requireOwnedWallet(replay.getFromWalletId(), callerUserId);
            return toTransferResponse(replay, true);
        }

        Wallet from = requireOwnedWallet(fromWalletId, callerUserId);
        Wallet to = requireWallet(toWalletId);

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw ApiException.unprocessable(
                    "CURRENCY_MISMATCH",
                    "Wallets must use the same currency. from="
                            + from.getCurrency() + ", to=" + to.getCurrency()
            );
        }

        from.debit(amount);
        to.credit(amount);
        walletRepository.save(from);
        walletRepository.save(to);

        WalletTransfer transfer;
        try {
            transfer = walletTransferRepository.saveAndFlush(new WalletTransfer(
                    idempotencyKey.trim(),
                    from.getId(),
                    to.getId(),
                    amount,
                    from.getBalance(),
                    to.getBalance()
            ));
        } catch (DataIntegrityViolationException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            WalletTransfer winner = walletTransferRepository.findByIdempotencyKey(idempotencyKey.trim())
                    .orElseThrow(() -> ex);
            return toTransferResponse(winner, true);
        }

        walletHistoryRepository.save(new WalletHistory(
                from.getId(), TransactionType.SENT, amount, from.getBalance(),
                from.getId(), to.getId(), transfer.getId(), null
        ));
        walletHistoryRepository.save(new WalletHistory(
                to.getId(), TransactionType.RECEIVED, amount, to.getBalance(),
                from.getId(), to.getId(), transfer.getId(), null
        ));

        return toTransferResponse(transfer, false);
    }

    /** ACTIVE → BLOCKED (no money moves). */
    @Transactional
    public WalletResponse block(Long walletId, Long callerUserId) {
        Wallet wallet = requireOwnedWallet(walletId, callerUserId);
        wallet.block();
        return toWalletResponse(walletRepository.save(wallet));
    }

    /** BLOCKED → ACTIVE. */
    @Transactional
    public WalletResponse unblock(Long walletId, Long callerUserId) {
        Wallet wallet = requireOwnedWallet(walletId, callerUserId);
        wallet.unblock();
        return toWalletResponse(walletRepository.save(wallet));
    }

    /** Close only if balance is zero. */
    @Transactional
    public WalletResponse close(Long walletId, Long callerUserId) {
        Wallet wallet = requireOwnedWallet(walletId, callerUserId);
        wallet.close();
        return toWalletResponse(walletRepository.save(wallet));
    }

    /**
     * Full statement for a wallet (newest first).
     * Interview limit: unbounded list — add pagination under real traffic.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long walletId, Long callerUserId) {
        requireOwnedWallet(walletId, callerUserId);
        return walletHistoryRepository.findByWalletIdOrderByCreatedAtDesc(walletId).stream()
                .map(entry -> toHistoryResponse(entry, null))
                .toList();
    }

    /**
     * Shared deposit/withdraw path.
     * History row carries the idempotency key (unique) so retries are safe.
     */
    private TransactionResponse applyDepositOrWithdraw(
            Long walletId,
            BigDecimal amount,
            String idempotencyKey,
            TransactionType type
    ) {
        requireIdempotencyKey(idempotencyKey);
        String key = idempotencyKey.trim();

        var existing = walletHistoryRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return toHistoryResponse(existing.get(), true);
        }

        Wallet wallet = requireWallet(walletId);
        if (type == TransactionType.DEPOSIT) {
            wallet.credit(amount);
        } else {
            wallet.debit(amount);
        }
        walletRepository.save(wallet);

        WalletHistory history;
        try {
            // flush now so unique violation is visible inside this TX
            history = walletHistoryRepository.saveAndFlush(new WalletHistory(
                    wallet.getId(),
                    type,
                    amount,
                    wallet.getBalance(),
                    null,
                    null,
                    null,
                    key
            ));
        } catch (DataIntegrityViolationException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            WalletHistory winner = walletHistoryRepository.findByIdempotencyKey(key)
                    .orElseThrow(() -> ex);
            return toHistoryResponse(winner, true);
        }

        return toHistoryResponse(history, false);
    }

    private Wallet requireWallet(Long walletId) {
        if (walletId == null || walletId <= 0) {
            throw new IllegalArgumentException("Wallet ID must be positive");
        }
        return walletRepository.findById(walletId)
                .orElseThrow(() -> ApiException.notFound("WALLET_NOT_FOUND", "Wallet not found: " + walletId));
    }

    /** Authenticated caller must own the wallet (permission ≠ ownership). */
    private Wallet requireOwnedWallet(Long walletId, Long callerUserId) {
        Wallet wallet = requireWallet(walletId);
        if (!wallet.getUserId().equals(callerUserId)) {
            throw ApiException.forbidden("Not your wallet");
        }
        return wallet;
    }

    private void requirePartyToTransfer(WalletTransfer transfer, Long callerUserId) {
        Wallet from = requireWallet(transfer.getFromWalletId());
        Wallet to = requireWallet(transfer.getToWalletId());
        if (!from.getUserId().equals(callerUserId) && !to.getUserId().equals(callerUserId)) {
            throw ApiException.forbidden("Not a party to this transfer");
        }
    }

    private static void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
    }

    private static WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreatedAt()
        );
    }

    private static TransactionResponse toHistoryResponse(WalletHistory entry, Boolean replayed) {
        return new TransactionResponse(
                entry.getId(),
                entry.getWalletId(),
                entry.getType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getFromWalletId(),
                entry.getToWalletId(),
                entry.getTransferId(),
                replayed,
                entry.getCreatedAt()
        );
    }

    private static TransferResponse toTransferResponse(WalletTransfer transfer, Boolean replayed) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromWalletId(),
                transfer.getToWalletId(),
                transfer.getAmount(),
                transfer.getFromBalanceAfter(),
                transfer.getToBalanceAfter(),
                replayed,
                transfer.getCreatedAt()
        );
    }
}
