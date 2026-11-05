package com.finflow.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Owns FinFlow ledger + money orchestration.
 * Wallet-service keeps materialised balances; this service posts balanced ledger entries
 * and runs deposit / withdraw / transfer (saga) with idempotency.
 */
@SpringBootApplication
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
