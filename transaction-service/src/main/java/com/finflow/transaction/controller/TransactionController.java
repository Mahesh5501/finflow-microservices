package com.finflow.transaction.controller;

import com.finflow.transaction.dto.TransactionResponse;
import com.finflow.transaction.dto.LedgerBalanceResponse;
import com.finflow.transaction.dto.MoneyRequest;
import com.finflow.transaction.dto.TransferRequest;
import com.finflow.transaction.security.AuthenticatedUser;
import com.finflow.transaction.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MoneyRequest request,
            Authentication authentication
    ) {
        return transactionService.deposit(caller(authentication).userId(), idempotencyKey, authorization, request);
    }

    @PostMapping("/transactions/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MoneyRequest request,
            Authentication authentication
    ) {
        return transactionService.withdraw(caller(authentication).userId(), idempotencyKey, authorization, request);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody TransferRequest request,
            Authentication authentication
    ) {
        return transactionService.transfer(caller(authentication).userId(), idempotencyKey, authorization, request);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> history(
            @RequestParam @NotNull Long walletId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication
    ) {
        return transactionService.history(walletId, caller(authentication).userId(), authorization);
    }

    /** Declared before /{transactionId} so "wallets" is not parsed as an id. */
    @GetMapping("/transactions/wallets/{walletId}/ledger-balance")
    public LedgerBalanceResponse ledgerBalance(
            @PathVariable Long walletId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication
    ) {
        return transactionService.ledgerBalance(walletId, caller(authentication).userId(), authorization);
    }

    @GetMapping("/transactions/{transactionId}")
    public TransactionResponse get(@PathVariable Long transactionId, Authentication authentication) {
        return transactionService.get(transactionId, caller(authentication).userId());
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
