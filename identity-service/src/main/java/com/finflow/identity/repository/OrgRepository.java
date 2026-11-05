package com.finflow.identity.repository;

import com.finflow.identity.entity.Org;
import org.springframework.data.jpa.repository.JpaRepository;

/** Organisation persistence. Empty body is normal — CRUD comes from {@link JpaRepository}. */
public interface OrgRepository extends JpaRepository<Org, Long> {
}
