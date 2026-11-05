package com.finflow.wallet.repository;

import com.finflow.wallet.entity.WalletHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Wallet statement reads + deposit/withdraw idempotency lookup. */
public interface WalletHistoryRepository extends JpaRepository<WalletHistory, Long> {

    List<WalletHistory> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    Optional<WalletHistory> findByIdempotencyKey(String idempotencyKey);
}
