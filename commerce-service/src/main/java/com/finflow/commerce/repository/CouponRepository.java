package com.finflow.commerce.repository;

import com.finflow.commerce.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByOrganisationIdAndCodeIgnoreCase(Long organisationId, String code);
}
