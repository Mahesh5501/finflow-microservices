package com.finflow.transaction.repository;

import com.finflow.transaction.entity.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {

    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    List<LedgerTransaction> findBySourceWalletIdOrDestinationWalletIdOrderByCreatedAtDesc(
            Long sourceWalletId,
            Long destinationWalletId
    );
}
