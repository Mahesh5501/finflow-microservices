package com.finflow.wallet.wallet.api.integration;

import com.finflow.wallet.security.AuthenticatedUser;
import com.finflow.wallet.wallet.application.WalletService;
import com.finflow.wallet.wallet.dto.MoneyRequest;
import com.finflow.wallet.wallet.dto.TransactionResponse;
import com.finflow.wallet.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Server-to-server wallet API for commerce checkout, refunds, and ops tools.
 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class IntegrationWalletController {

    private final WalletService walletService;

    public IntegrationWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/users/{userId}/wallet")
    public WalletResponse getWalletByUser(Authentication authentication, @PathVariable @Positive Long userId) {
        return walletService.getWalletByUserId(userId, caller(authentication).userId());
    }

    @GetMapping("/wallets/{walletId}")
    public WalletResponse getWalletById(Authentication authentication, @PathVariable @Positive Long walletId) {
        return walletService.getWalletById(walletId, caller(authentication).userId());
    }

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

    @GetMapping("/wallets/{walletId}/transactions")
    public List<TransactionResponse> transactions(
            Authentication authentication,
            @PathVariable @Positive Long walletId
    ) {
        return walletService.getTransactions(walletId, caller(authentication).userId());
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
