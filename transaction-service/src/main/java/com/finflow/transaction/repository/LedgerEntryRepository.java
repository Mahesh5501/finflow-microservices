package com.finflow.transaction.repository;

import com.finflow.transaction.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionIdOrderByPostedAtAsc(Long transactionId);

    @Query("select coalesce(sum(e.signedAmount), 0) from LedgerEntry e where e.walletId = :walletId")
    BigDecimal sumSignedAmountByWalletId(@Param("walletId") Long walletId);
}
