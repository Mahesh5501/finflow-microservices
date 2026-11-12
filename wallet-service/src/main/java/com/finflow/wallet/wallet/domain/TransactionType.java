package com.finflow.wallet.wallet.domain;

/**
 * Activity labels on {@code wallet_history} (same vocabulary many fintech UIs use).
 * SENT/RECEIVED always include fromWalletId → toWalletId on the history row.
 */
public enum TransactionType {
    /** Manual or system credit (e.g. commerce refund). Prefer {@link #TOP_UP} for customer add-money. */
    DEPOSIT,
    WITHDRAW,
    /** Customer add-money after an external payment provider confirms (card/UPI stub or real PG). */
    TOP_UP,
    SENT,
    RECEIVED
}
