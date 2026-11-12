package com.finflow.wallet.wallet.persistence;

import com.finflow.wallet.wallet.domain.WalletTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTransferRepository extends JpaRepository<WalletTransfer, Long> {

    Optional<WalletTransfer> findByIdempotencyKey(String idempotencyKey);
}
