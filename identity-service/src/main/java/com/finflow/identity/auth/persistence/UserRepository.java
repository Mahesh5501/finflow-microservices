package com.finflow.identity.auth.persistence;

import com.finflow.identity.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User persistence through Spring Data {@code JpaRepository} + derived query methods.
 * Unique email is enforced in DB ({@code uk_app_user_email}).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
