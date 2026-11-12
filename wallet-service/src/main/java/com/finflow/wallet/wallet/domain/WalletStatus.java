package com.finflow.wallet.wallet.domain;

/**
 * Wallet lifecycle: ACTIVE (normal), BLOCKED (no money moves), CLOSED (done).
 */
public enum WalletStatus {
    ACTIVE,
    BLOCKED,
    CLOSED
}
