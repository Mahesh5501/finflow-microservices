package com.finflow.identity.repository;

import com.finflow.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User persistence. Market: Spring Data {@code JpaRepository} + derived query methods.
 * Unique email is enforced in DB ({@code uk_app_user_email}).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
