package com.finflow.commerce.repository;

import com.finflow.commerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerUserIdAndOrganisationId(Long customerUserId, Long organisationId);

    List<Cart> findByUpdatedAtBefore(Instant updatedBefore);
}
