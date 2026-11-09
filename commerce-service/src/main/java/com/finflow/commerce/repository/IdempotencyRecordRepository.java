package com.finflow.commerce.repository;

import com.finflow.commerce.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Database access for idempotency records.
 *
 * The primary key of IdempotencyRecord is a String,
 * because the client supplies the idempotency key.
 */
public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, String> {

    /**
     * Finds an idempotency record by its key.
     *
     * Optional.empty() means this key has not been stored.
     * Optional containing a record means it was already used.
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(
            String idempotencyKey
    );
}