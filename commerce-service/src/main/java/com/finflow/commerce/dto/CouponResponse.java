package com.finflow.commerce.dto;

import com.finflow.commerce.entity.Coupon;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        Long couponId,
        Long organisationId,
        String code,
        BigDecimal percentOff,
        boolean active,
        Instant expiresAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getCouponId(),
                coupon.getOrganisationId(),
                coupon.getCode(),
                coupon.getPercentOff(),
                coupon.isActive(),
                coupon.getExpiresAt()
        );
    }
}
