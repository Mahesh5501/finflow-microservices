package com.finflow.commerce.order.persistence;

import com.finflow.commerce.order.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Database access for idempotency records.
 * The primary key of IdempotencyRecord is a String, because the client supplies the idempotency key.
 *
 * Persistence layer interface called by application services. Spring Data discovers
 * it below the application root and supplies a JPA proxy; @Repository is not required
 * on this interface (it is useful on a hand-written DAO for exception translation).
 * Removing JpaRepository inheritance would remove that generated CRUD contract.
 * Derived queries use Java property names, not SQL columns; custom JPQL must do the
 * same. Repositories do not authenticate callers: application services must supply
 * owner/seller constraints and define transactions spanning multiple repositories.
 * Native SQL is an alternative for database-specific operations, at a portability cost.
 */
public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, String> {

    /**
     * Finds an idempotency record by its key.
     * @param idempotencyKey the idempotency key.
     * @return the idempotency record with the given idempotency key.
     * Optional.empty() means this key has not been stored.
     * Optional containing a record means it was already used.
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(
            String idempotencyKey
    );
}