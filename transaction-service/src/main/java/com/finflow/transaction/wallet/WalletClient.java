package com.finflow.transaction.wallet;

import java.math.BigDecimal;

/**
 * Balance mutations live in wallet-service.
 * Transaction-service never updates wallet tables directly.
 */
public interface WalletClient {

    WalletAccount getWallet(Long walletId, String accessToken);

    void deposit(Long walletId, BigDecimal amount, String idempotencyKey, String accessToken);

    void withdraw(Long walletId, BigDecimal amount, String idempotencyKey, String accessToken);
}
