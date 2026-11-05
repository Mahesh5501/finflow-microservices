package com.finflow.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(
        name = "coupons",
        uniqueConstraints = @UniqueConstraint(name = "uk_coupon_org_code", columnNames = {"organisation_id", "code"})
)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(nullable = false, length = 40)
    private String code;

    /** Percent off, e.g. 10.00 = 10%. */
    @Column(name = "percent_off", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentOff;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Coupon() {
    }

    public Coupon(Long organisationId, String code, BigDecimal percentOff, Instant expiresAt) {
        this.organisationId = organisationId;
        this.code = code.trim().toUpperCase();
        this.percentOff = percentOff;
        this.active = true;
        this.expiresAt = expiresAt;
    }

    public BigDecimal apply(BigDecimal subtotal) {
        if (!active) {
            throw new IllegalStateException("Coupon inactive");
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            throw new IllegalStateException("Coupon expired");
        }
        BigDecimal factor = BigDecimal.ONE.subtract(
                percentOff.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );
        return subtotal.multiply(factor).setScale(4, RoundingMode.HALF_UP);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getCouponId() {
        return couponId;
    }

    public Long getOrganisationId() {
        return organisationId;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getPercentOff() {
        return percentOff;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
