package com.finflow.wallet.repository;

import com.finflow.wallet.entity.WalletTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTransferRepository extends JpaRepository<WalletTransfer, Long> {

    Optional<WalletTransfer> findByIdempotencyKey(String idempotencyKey);
}
