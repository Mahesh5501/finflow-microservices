package com.finflow.wallet.controller;

import com.finflow.wallet.dto.MoneyRequest;
import com.finflow.wallet.dto.TransactionResponse;
import com.finflow.wallet.dto.TransferRequest;
import com.finflow.wallet.dto.TransferResponse;
import com.finflow.wallet.dto.WalletRequest;
import com.finflow.wallet.dto.WalletResponse;
import com.finflow.wallet.security.AuthenticatedUser;
import com.finflow.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP edge for wallet APIs (presentation layer).
 *
 * <h2>Auth</h2>
 * Bearer access JWT from identity-service (RS256 verified via JWKS).
 * Permissions enforced in {@code SecurityConfig}; ownership in {@link WalletService}.
 *
 * <h2>Money APIs &amp; Idempotency-Key</h2>
 * Deposit, withdraw, and transfer require header {@code Idempotency-Key}.
 *
 * <pre>
 * POST   /api/v1/wallets
 * GET    /api/v1/wallets/{walletId}
 * GET    /api/v1/users/{userId}/wallet
 * POST   /api/v1/wallets/{walletId}/deposits|withdrawals|block|unblock|close
 * GET    /api/v1/wallets/{walletId}/transactions
 * POST   /api/v1/transfers
 * GET    /api/v1/transfers/{transferId}
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /** Create wallet for the JWT subject (balance 0). 409 if already exists. */
    @PostMapping("/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(
            Authentication authentication,
            @Valid @RequestBody WalletRequest request
    ) {
        return walletService.createWallet(caller(authentication).userId(), request.currency());
    }

    /** Read current balance and status (must own wallet). */
    @GetMapping("/wallets/{walletId}")
    public WalletResponse getWallet(Authentication authentication, @PathVariable @Positive Long walletId) {
        return walletService.getWalletById(walletId, caller(authentication).userId());
    }

    /** Lookup own wallet by user id (path must match JWT subject). */
    @GetMapping("/users/{userId}/wallet")
    public WalletResponse getWalletByUser(Authentication authentication, @PathVariable @Positive Long userId) {
        return walletService.getWalletByUserId(userId, caller(authentication).userId());
    }

    /** Credit wallet. Same Idempotency-Key → same result, no double credit. */
    @PostMapping("/wallets/{walletId}/deposits")
    public TransactionResponse deposit(
            Authentication authentication,
            @PathVariable @Positive Long walletId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody MoneyRequest request
    ) {
        return walletService.deposit(
                walletId, caller(authentication).userId(), request.amount(), idempotencyKey
        );
    }

    /** Debit wallet. Fails with 422 if balance too low. */
    @PostMapping("/wallets/{walletId}/withdrawals")
    public TransactionResponse withdraw(
            Authentication authentication,
            @PathVariable @Positive Long walletId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody MoneyRequest request
    ) {
        return walletService.withdraw(
                walletId, caller(authentication).userId(), request.amount(), idempotencyKey
        );
    }

    /** Statement / activity for one wallet (newest first). */
    @GetMapping("/wallets/{walletId}/transactions")
    public List<TransactionResponse> getTransactions(
            Authentication authentication,
            @PathVariable @Positive Long walletId
    ) {
        return walletService.getTransactions(walletId, caller(authentication).userId());
    }

    /** Risk hold: blocks deposits, withdrawals, and transfers. */
    @PostMapping("/wallets/{walletId}/block")
    public WalletResponse block(Authentication authentication, @PathVariable @Positive Long walletId) {
        return walletService.block(walletId, caller(authentication).userId());
    }

    /** Clear risk hold. */
    @PostMapping("/wallets/{walletId}/unblock")
    public WalletResponse unblock(Authentication authentication, @PathVariable @Positive Long walletId) {
        return walletService.unblock(walletId, caller(authentication).userId());
    }

    /** Close wallet only when balance is zero. */
    @PostMapping("/wallets/{walletId}/close")
    public WalletResponse close(Authentication authentication, @PathVariable @Positive Long walletId) {
        return walletService.close(walletId, caller(authentication).userId());
    }

    /**
     * P2P transfer fromWallet → toWallet in one DB transaction.
     * Caller must own {@code fromWalletId}.
     */
    @PostMapping("/transfers")
    public TransferResponse transfer(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        return walletService.transfer(
                request.fromWalletId(),
                request.toWalletId(),
                request.amount(),
                idempotencyKey,
                caller(authentication).userId()
        );
    }

    /** Fetch one transfer by id (must be party to the transfer). */
    @GetMapping("/transfers/{transferId}")
    public TransferResponse getTransfer(
            Authentication authentication,
            @PathVariable @Positive Long transferId
    ) {
        return walletService.getTransfer(transferId, caller(authentication).userId());
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
