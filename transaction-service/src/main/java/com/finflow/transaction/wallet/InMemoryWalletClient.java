package com.finflow.transaction.wallet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process balances for tests / offline demo (wallet.provider=simulated).
 * Seeds wallets 1 and 2 for actor ownership checks.
 */
@Component
@ConditionalOnProperty(name = "wallet.provider", havingValue = "simulated", matchIfMissing = false)
public class InMemoryWalletClient implements WalletClient {

    private final Map<Long, MutableWallet> wallets = new ConcurrentHashMap<>();
    private final Map<String, Boolean> idempotency = new ConcurrentHashMap<>();

    public InMemoryWalletClient() {
        wallets.put(1L, new MutableWallet(1L, 100L, new BigDecimal("0.00"), "INR", "ACTIVE"));
        wallets.put(2L, new MutableWallet(2L, 200L, new BigDecimal("0.00"), "INR", "ACTIVE"));
    }

    /** Test helper — top up without going through transaction-service. */
    public void seedBalance(Long walletId, BigDecimal balance) {
        MutableWallet w = require(walletId);
        w.balance = normalize(balance);
    }

    public void seedOwner(Long walletId, Long userId) {
        MutableWallet w = require(walletId);
        w.userId = userId;
    }

    /** Clears idempotency keys so each test can reuse logical keys if needed. */
    public void resetIdempotency() {
        idempotency.clear();
    }

    @Override
    public WalletAccount getWallet(Long walletId, String accessToken) {
        MutableWallet w = require(walletId);
        return new WalletAccount(w.id, w.userId, w.balance, w.currency, w.status); // walletId == id
    }

    @Override
    public void deposit(Long walletId, BigDecimal amount, String idempotencyKey, String accessToken) {
        if (idempotency.putIfAbsent(idempotencyKey, Boolean.TRUE) != null) {
            return;
        }
        MutableWallet w = requireActive(walletId);
        w.balance = w.balance.add(normalize(amount));
    }

    @Override
    public void withdraw(Long walletId, BigDecimal amount, String idempotencyKey, String accessToken) {
        if (idempotency.putIfAbsent(idempotencyKey, Boolean.TRUE) != null) {
            return;
        }
        MutableWallet w = requireActive(walletId);
        BigDecimal normalized = normalize(amount);
        if (w.balance.compareTo(normalized) < 0) {
            idempotency.remove(idempotencyKey);
            throw new WalletClientException(
                    "INSUFFICIENT_BALANCE",
                    "Insufficient balance. available=" + w.balance + ", requested=" + normalized
            );
        }
        w.balance = w.balance.subtract(normalized);
    }

    private MutableWallet require(Long walletId) {
        MutableWallet w = wallets.get(walletId);
        if (w == null) {
            throw new WalletClientException("WALLET_NOT_FOUND", "Wallet not found: " + walletId);
        }
        return w;
    }

    private MutableWallet requireActive(Long walletId) {
        MutableWallet w = require(walletId);
        if (!"ACTIVE".equals(w.status)) {
            throw new WalletClientException("WALLET_INACTIVE", "Wallet is not ACTIVE");
        }
        return w;
    }

    private static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class MutableWallet {
        final Long id;
        Long userId;
        BigDecimal balance;
        final String currency;
        final String status;

        MutableWallet(Long id, Long userId, BigDecimal balance, String currency, String status) {
            this.id = id;
            this.userId = userId;
            this.balance = balance;
            this.currency = currency;
            this.status = status;
        }
    }
}
