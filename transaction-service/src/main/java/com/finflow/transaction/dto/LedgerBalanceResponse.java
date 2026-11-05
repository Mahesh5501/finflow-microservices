package com.finflow.transaction.dto;

import java.math.BigDecimal;

/** Reconcile materialised wallet balance against posted ledger entries. */
public record LedgerBalanceResponse(Long walletId, BigDecimal ledgerBalance) {
}
