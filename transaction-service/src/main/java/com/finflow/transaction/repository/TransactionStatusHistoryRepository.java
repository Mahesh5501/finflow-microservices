package com.finflow.transaction.repository;

import com.finflow.transaction.entity.TransactionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionStatusHistoryRepository extends JpaRepository<TransactionStatusHistory, Long> {

    List<TransactionStatusHistory> findByTransactionIdOrderByChangedAtAsc(Long transactionId);
}
