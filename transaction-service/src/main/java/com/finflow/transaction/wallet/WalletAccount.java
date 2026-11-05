package com.finflow.transaction.wallet;

import java.math.BigDecimal;

/** Mirrors wallet-service WalletResponse field names for RestClient JSON binding. */
public record WalletAccount(
        Long walletId,
        Long userId,
        BigDecimal balance,
        String currency,
        String status
) {
}
