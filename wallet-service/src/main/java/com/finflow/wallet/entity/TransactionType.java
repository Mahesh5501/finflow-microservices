package com.finflow.wallet.entity;

/**
 * Activity labels on {@code wallet_history} (same vocabulary many fintech UIs use).
 * SENT/RECEIVED always include fromWalletId → toWalletId on the history row.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    SENT,
    RECEIVED
}
