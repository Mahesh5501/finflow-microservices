package com.finflow.wallet.wallet.persistence;

import com.finflow.wallet.wallet.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence port for {@link Wallet}.
 *
 * <p><b>Why Spring Data JPA?</b> CRUD and derived queries are enough for this aggregate.
 * Custom SQL can be added later for reconciliation reports without changing the service API.
 *
 * <p><b>Risk:</b> Derived query names must match entity fields. Prefer explicit
 * {@code @Query} when the SQL becomes non-obvious.
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
