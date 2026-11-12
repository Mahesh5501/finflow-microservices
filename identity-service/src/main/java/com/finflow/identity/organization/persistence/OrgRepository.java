package com.finflow.identity.organization.persistence;

import com.finflow.identity.organization.domain.Org;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Organisation persistence.
 * The pessimistic write lock serializes membership administration operations for the same organisation.
 */
public interface OrgRepository extends JpaRepository<Org, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Org o where o.id = :orgId")
    Optional<Org> findByIdForUpdate(@Param("orgId") Long orgId);
}
