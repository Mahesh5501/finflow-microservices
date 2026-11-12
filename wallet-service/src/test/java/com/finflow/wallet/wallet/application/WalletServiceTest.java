package com.finflow.wallet.wallet.application;

import com.finflow.wallet.wallet.dto.TopUpRequest;
import com.finflow.wallet.wallet.dto.TransactionResponse;
import com.finflow.wallet.wallet.dto.TransferResponse;
import com.finflow.wallet.wallet.dto.WalletResponse;
import com.finflow.wallet.wallet.domain.TransactionType;
import com.finflow.wallet.payment.TopUpPaymentMethod;
import com.finflow.wallet.wallet.domain.WalletStatus;
import com.finflow.wallet.exception.ApiException;
import com.finflow.wallet.wallet.persistence.WalletHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletHistoryRepository walletHistoryRepository;

    @Test
    void createWalletStartsAtZero() {
        WalletResponse wallet = walletService.createWallet(1001L, "inr");

        assertEquals(0, new BigDecimal("0.00").compareTo(wallet.balance()));
        assertEquals("INR", wallet.currency());
    }

    @Test
    void createWalletRejectsDuplicateUser() {
        walletService.createWallet(1002L, "INR");
        ApiException ex = assertThrows(ApiException.class, () -> walletService.createWallet(1002L, "INR"));
        assertEquals("WALLET_ALREADY_EXISTS", ex.getCode());
    }

    @Test
    void topUpCreditsWalletAfterStubPaymentCapture() {
        WalletResponse wallet = walletService.createWallet(1099L, "INR");

        var topUp = walletService.topUp(
                wallet.walletId(),
                1099L,
                new TopUpRequest(new BigDecimal("250.00"), TopUpPaymentMethod.UPI, "pg-session-abc"),
                "topup-key-1"
        );

        assertFalse(topUp.replayed());
        assertEquals(0, new BigDecimal("250.00").compareTo(topUp.balanceAfter()));
        assertTrue(topUp.paymentProviderReference().startsWith("stub-"));
        assertEquals(
                TransactionType.TOP_UP,
                walletHistoryRepository.findByIdempotencyKey("topup-key-1").orElseThrow().getType()
        );
    }

    @Test
    void depositIncreasesBalanceAndWritesHistory() {
        WalletResponse wallet = walletService.createWallet(1003L, "INR");

        TransactionResponse tx = walletService.deposit(
                wallet.walletId(), 1003L, new BigDecimal("500.00"), "dep-1"
        );

        assertFalse(tx.replayed());
        assertEquals(0, new BigDecimal("500.00").compareTo(tx.balanceAfter()));
        assertEquals(1, walletHistoryRepository.findByWalletIdOrderByCreatedAtDesc(wallet.walletId()).size());
    }

    @Test
    void depositWithSameIdempotencyKeyDoesNotDoubleCredit() {
        WalletResponse wallet = walletService.createWallet(1004L, "INR");

        TransactionResponse first = walletService.deposit(
                wallet.walletId(), 1004L, new BigDecimal("100.00"), "dep-same"
        );
        TransactionResponse second = walletService.deposit(
                wallet.walletId(), 1004L, new BigDecimal("100.00"), "dep-same"
        );

        assertFalse(first.replayed());
        assertTrue(second.replayed());
        assertEquals(first.id(), second.id());
        assertEquals(0, new BigDecimal("100.00").compareTo(
                walletService.getWalletById(wallet.walletId(), 1004L).balance()
        ));
        assertEquals(1, walletHistoryRepository.findByWalletIdOrderByCreatedAtDesc(wallet.walletId()).size());
    }

    @Test
    void withdrawDecreasesBalance() {
        WalletResponse wallet = walletService.createWallet(1005L, "INR");
        walletService.deposit(wallet.walletId(), 1005L, new BigDecimal("200.00"), "dep-w");

        TransactionResponse withdraw = walletService.withdraw(
                wallet.walletId(), 1005L, new BigDecimal("50.00"), "wd-1"
        );

        assertEquals(0, new BigDecimal("150.00").compareTo(withdraw.balanceAfter()));
    }

    @Test
    void withdrawFailsWhenInsufficientBalance() {
        WalletResponse wallet = walletService.createWallet(1006L, "INR");
        walletService.deposit(wallet.walletId(), 1006L, new BigDecimal("20.00"), "dep-small");

        ApiException ex = assertThrows(
                ApiException.class,
                () -> walletService.withdraw(wallet.walletId(), 1006L, new BigDecimal("50.00"), "wd-fail")
        );
        assertEquals("INSUFFICIENT_BALANCE", ex.getCode());
        assertEquals(0, new BigDecimal("20.00").compareTo(
                walletService.getWalletById(wallet.walletId(), 1006L).balance()
        ));
    }

    @Test
    void transferMovesMoneyBetweenWallets() {
        WalletResponse from = walletService.createWallet(2001L, "INR");
        WalletResponse to = walletService.createWallet(2002L, "INR");
        walletService.deposit(from.walletId(), 2001L, new BigDecimal("300.00"), "dep-tr-src");

        TransferResponse transfer = walletService.transfer(
                from.walletId(), to.walletId(), new BigDecimal("100.00"), "tr-1", 2001L
        );

        assertFalse(transfer.replayed());
        assertEquals(from.walletId(), transfer.fromWalletId());
        assertEquals(to.walletId(), transfer.toWalletId());
        assertEquals(0, new BigDecimal("200.00").compareTo(transfer.fromBalanceAfter()));
        assertEquals(0, new BigDecimal("100.00").compareTo(transfer.toBalanceAfter()));

        var fromHistory = walletService.getTransactions(from.walletId(), 2001L);
        var sent = fromHistory.stream().filter(h -> h.type().name().equals("SENT")).findFirst().orElseThrow();
        assertEquals(from.walletId(), sent.fromWalletId());
        assertEquals(to.walletId(), sent.toWalletId());
    }

    @Test
    void transferReplayDoesNotDoubleMove() {
        WalletResponse from = walletService.createWallet(2003L, "INR");
        WalletResponse to = walletService.createWallet(2004L, "INR");
        walletService.deposit(from.walletId(), 2003L, new BigDecimal("100.00"), "dep-tr-replay");

        TransferResponse first = walletService.transfer(
                from.walletId(), to.walletId(), new BigDecimal("40.00"), "tr-same", 2003L
        );
        TransferResponse second = walletService.transfer(
                from.walletId(), to.walletId(), new BigDecimal("40.00"), "tr-same", 2003L
        );

        assertFalse(first.replayed());
        assertTrue(second.replayed());
        assertEquals(first.transferId(), second.transferId());
        assertEquals(0, new BigDecimal("60.00").compareTo(
                walletService.getWalletById(from.walletId(), 2003L).balance()
        ));
        assertEquals(0, new BigDecimal("40.00").compareTo(
                walletService.getWalletById(to.walletId(), 2004L).balance()
        ));
    }

    @Test
    void transferRejectsDifferentCurrency() {
        WalletResponse from = walletService.createWallet(2005L, "INR");
        WalletResponse to = walletService.createWallet(2006L, "USD");
        walletService.deposit(from.walletId(), 2005L, new BigDecimal("50.00"), "dep-fx");

        ApiException ex = assertThrows(
                ApiException.class,
                () -> walletService.transfer(
                        from.walletId(), to.walletId(), new BigDecimal("10.00"), "tr-fx", 2005L
                )
        );
        assertEquals("CURRENCY_MISMATCH", ex.getCode());
    }

    @Test
    void transferRejectsWhenCallerDoesNotOwnFromWallet() {
        WalletResponse from = walletService.createWallet(2010L, "INR");
        WalletResponse to = walletService.createWallet(2011L, "INR");
        walletService.deposit(from.walletId(), 2010L, new BigDecimal("50.00"), "dep-own");

        ApiException ex = assertThrows(
                ApiException.class,
                () -> walletService.transfer(
                        from.walletId(), to.walletId(), new BigDecimal("10.00"), "tr-steal", 2011L
                )
        );
        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void blockStopsDeposit() {
        WalletResponse wallet = walletService.createWallet(3001L, "INR");
        walletService.block(wallet.walletId(), 3001L);

        assertEquals(WalletStatus.BLOCKED, walletService.getWalletById(wallet.walletId(), 3001L).status());
        ApiException ex = assertThrows(
                ApiException.class,
                () -> walletService.deposit(wallet.walletId(), 3001L, new BigDecimal("10.00"), "dep-blocked")
        );
        assertEquals("WALLET_NOT_ACTIVE", ex.getCode());
    }

    @Test
    void unblockAllowsDepositAgain() {
        WalletResponse wallet = walletService.createWallet(3002L, "INR");
        walletService.block(wallet.walletId(), 3002L);
        walletService.unblock(wallet.walletId(), 3002L);

        TransactionResponse tx = walletService.deposit(
                wallet.walletId(), 3002L, new BigDecimal("10.00"), "dep-after"
        );
        assertEquals(0, new BigDecimal("10.00").compareTo(tx.balanceAfter()));
    }

    @Test
    void closeRequiresZeroBalance() {
        WalletResponse wallet = walletService.createWallet(3003L, "INR");
        walletService.deposit(wallet.walletId(), 3003L, new BigDecimal("5.00"), "dep-close");

        assertThrows(IllegalArgumentException.class, () -> walletService.close(wallet.walletId(), 3003L));

        walletService.withdraw(wallet.walletId(), 3003L, new BigDecimal("5.00"), "wd-close");
        assertEquals(WalletStatus.CLOSED, walletService.close(wallet.walletId(), 3003L).status());
    }
}
