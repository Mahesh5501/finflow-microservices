package com.finflow.wallet.wallet.api.customer;

import com.finflow.wallet.security.AuthenticatedUser;
import com.finflow.wallet.wallet.application.WalletService;
import com.finflow.wallet.wallet.dto.TopUpRequest;
import com.finflow.wallet.wallet.dto.TopUpResponse;
import com.finflow.wallet.wallet.dto.TransactionResponse;
import com.finflow.wallet.wallet.dto.WalletRequest;
import com.finflow.wallet.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Shopper wallet API — same audience style as commerce {@code /api/v1/customer/**}.
 */
@RestController
@RequestMapping("/api/v1/customer/wallet")
@Validated
public class CustomerWalletController {

    private final WalletService walletService;

    public CustomerWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse getWallet(Authentication authentication) {
        Long userId = caller(authentication).userId();
        return walletService.getWalletByUserId(userId, userId);
    }

    @PostMapping
    public ResponseEntity<WalletResponse> openWallet(
            Authentication authentication,
            @Valid @RequestBody WalletRequest request
    ) {
        Long userId = caller(authentication).userId();
        boolean existed = walletService.hasWallet(userId);
        WalletResponse wallet = walletService.ensureWallet(userId, request.currency());
        return existed
                ? ResponseEntity.ok(wallet)
                : ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @PostMapping("/top-ups")
    public TopUpResponse topUp(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody TopUpRequest request
    ) {
        Long userId = caller(authentication).userId();
        WalletResponse wallet = walletService.ensureWallet(userId, walletService.defaultCurrency());
        return walletService.topUp(wallet.walletId(), userId, request, idempotencyKey);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(Authentication authentication) {
        Long userId = caller(authentication).userId();
        WalletResponse wallet = walletService.getWalletByUserId(userId, userId);
        return walletService.getTransactions(wallet.walletId(), userId);
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
