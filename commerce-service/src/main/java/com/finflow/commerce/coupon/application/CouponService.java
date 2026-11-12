package com.finflow.commerce.coupon.application;

import com.finflow.commerce.coupon.domain.Coupon;
import com.finflow.commerce.coupon.dto.CouponRequest;
import com.finflow.commerce.coupon.dto.CouponResponse;
import com.finflow.commerce.coupon.persistence.CouponRepository;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.seller.MerchantSellerAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Coupon application boundary, called by MerchantCouponController to create coupons
 * and by OrderPlacementService to price a seller's checkout. Keeping these use cases
 * here stops the order facade from becoming a catalogue/coupon management service.
 * The repository owns queries; Coupon owns activity, expiry and discount arithmetic.
 * No customer coupon endpoint is needed: checkout supplies the optional code.
 *
 * <p>@Service makes this concrete class injectable; an explicit @Bean would also
 * work, but without either Spring cannot wire the controllers. Constructor injection
 * makes the repository mandatory and permits isolated tests without a Spring context.
 * The concrete service is enough here; a second interface would add no useful seam.
 */
@Service
public class CouponService {

    // Persists merchant-owned coupons and resolves a code within one seller only.
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * Merchant creation: authenticate in HTTP, check membership here, then check for
     * duplicates, save and map to a DTO. A duplicate returns COUPON_EXISTS; the table's
     * unique constraint remains the final protection for concurrent creation attempts.
     * @Transactional joins an existing transaction (REQUIRED) or opens one; removing
     * it would leave the lookup and save without a shared application unit of work.
     */
    @Transactional
    public CouponResponse createCoupon(
            List<Long> callerOrgIds,
            CouponRequest request
    ) {

        // Seller comes from the token — never from the request body (prevents cross-shop coupon creation).
        Long sellerId = MerchantSellerAccess.requireSellerId(callerOrgIds);

        if (couponRepository.findByOrganisationIdAndCodeIgnoreCase(
                sellerId,
                request.code()
        ).isPresent()) {
            throw ApiException.conflict(
                    "COUPON_EXISTS",
                    "Coupon code already exists"
            );
        }

        Coupon coupon = new Coupon(
                sellerId,
                request.code(),
                request.percentOff(),
                request.expiresAt()
        );

        // Persist the coupon and convert the saved entity into a response.
        return CouponResponse.from(couponRepository.save(coupon));
    }

    /**
     * Called during placement, after stock and item snapshots have been prepared.
     * REQUIRED joins the placement transaction, so a missing coupon rolls back the
     * stock reservation and order together. No separate transaction
     * (REQUIRES_NEW) is appropriate: coupon pricing is part of this checkout.
     * Returns the domain object for its apply() operation and canonical stored code.
     * It never crosses the HTTP boundary; merchant creation still returns a DTO.
     * Buyers need not belong to the seller; the product-derived seller scopes lookup.
     */
    @Transactional(readOnly = true)
    public Coupon findForCheckout(Long organisationId, String code) {
        return couponRepository.findByOrganisationIdAndCodeIgnoreCase(
                        organisationId, code.trim())
                .orElseThrow(() -> ApiException.notFound(
                        "COUPON_NOT_FOUND", "Coupon not found: " + code));
    }

    private static void requireOrgAccess(List<Long> callerOrgIds, Long organisationId) {
        if (callerOrgIds == null || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden("No access to organisation " + organisationId);
        }
    }
}
