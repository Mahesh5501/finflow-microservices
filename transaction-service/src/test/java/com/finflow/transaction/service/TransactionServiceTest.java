package com.finflow.transaction.service;

import com.finflow.transaction.dto.TransactionResponse;
import com.finflow.transaction.dto.LedgerBalanceResponse;
import com.finflow.transaction.dto.MoneyRequest;
import com.finflow.transaction.dto.TransferRequest;
import com.finflow.transaction.entity.TransactionStatus;
import com.finflow.transaction.entity.TransactionType;
import com.finflow.transaction.repository.LedgerTransactionRepository;
import com.finflow.transaction.repository.LedgerEntryRepository;
import com.finflow.transaction.repository.TransactionStatusHistoryRepository;
import com.finflow.transaction.wallet.InMemoryWalletClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private InMemoryWalletClient walletClient;

    @Autowired
    private LedgerTransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TransactionStatusHistoryRepository statusHistoryRepository;

    @BeforeEach
    void seed() {
        statusHistoryRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        walletClient.resetIdempotency();
        walletClient.seedOwner(1L, 100L);
        walletClient.seedOwner(2L, 200L);
        walletClient.seedBalance(1L, new BigDecimal("0.00"));
        walletClient.seedBalance(2L, new BigDecimal("0.00"));
    }

    @Test
    void deposit_postsCreditLedger_andIsIdempotent() {
        TransactionResponse first = transactionService.deposit(
                100L, "dep-1", "Bearer t", new MoneyRequest(1L, new BigDecimal("50.00"))
        );
        TransactionResponse second = transactionService.deposit(
                100L, "dep-1", "Bearer t", new MoneyRequest(1L, new BigDecimal("50.00"))
        );

        assertEquals(TransactionStatus.SUCCESS, first.status());
        assertEquals(TransactionType.DEPOSIT, first.type());
        assertEquals(first.transactionId(), second.transactionId());
        assertTrue(Boolean.TRUE.equals(second.replayed()));
        assertEquals(1, first.ledgerEntries().size());
        assertEquals(0, new BigDecimal("50.00").compareTo(first.ledgerEntries().getFirst().signedAmount()));

        LedgerBalanceResponse sum = transactionService.ledgerBalance(1L, 100L, "Bearer t");
        assertEquals(0, new BigDecimal("50.00").compareTo(sum.ledgerBalance()));
    }

    @Test
    void withdraw_insufficient_marksFailed_withoutLedger() {
        transactionService.deposit(100L, "dep-w", "Bearer t", new MoneyRequest(1L, new BigDecimal("10.00")));

        TransactionResponse failed = transactionService.withdraw(
                100L, "wd-fail", "Bearer t", new MoneyRequest(1L, new BigDecimal("99.00"))
        );

        assertEquals(TransactionStatus.FAILED, failed.status());
        assertEquals("INSUFFICIENT_BALANCE", failed.failureCode());
        assertTrue(failed.ledgerEntries() == null || failed.ledgerEntries().isEmpty());
    }

    @Test
    void transfer_balancesLedgerToZero() {
        transactionService.deposit(100L, "dep-t", "Bearer t", new MoneyRequest(1L, new BigDecimal("40.00")));

        TransactionResponse transfer = transactionService.transfer(
                100L,
                "xfer-1",
                "Bearer t",
                new TransferRequest(1L, 2L, new BigDecimal("15.00"))
        );

        assertEquals(TransactionStatus.SUCCESS, transfer.status());
        assertEquals(2, transfer.ledgerEntries().size());
        BigDecimal net = transfer.ledgerEntries().stream()
                .map(e -> e.signedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, BigDecimal.ZERO.compareTo(net));
        assertNotNull(transfer.transactionId());

        assertEquals(0, new BigDecimal("25.00").compareTo(transactionService.ledgerBalance(1L, 100L, "Bearer t").ledgerBalance()));
        // destination owner is 200 — read with that actor
        walletClient.seedOwner(2L, 200L);
        assertEquals(0, new BigDecimal("15.00").compareTo(transactionService.ledgerBalance(2L, 200L, "Bearer t").ledgerBalance()));
    }
}
