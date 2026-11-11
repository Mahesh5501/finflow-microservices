package com.finflow.commerce.coupon.api.merchant;

import com.finflow.commerce.coupon.dto.CouponRequest;
import com.finflow.commerce.coupon.dto.CouponResponse;
import com.finflow.commerce.coupon.application.CouponService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature API layer for merchant coupon creation. Called by Spring MVC after the
 * SecurityFilterChain authenticates/authorizes; constructor injection supplies CouponService.
 * Flow: bind HTTP/validate DTO -> extract caller -> application use case -> map domain
 * inside service -> CouponResponse -> JSON. No repository or money logic belongs here.
 *
 * @RestController combines component discovery and response-body serialization;
 * without it (or @Controller plus @ResponseBody) these JSON handlers are not registered.
 * @RequestMapping and verb mappings preserve the existing audience-specific routes.
 * @Valid on request bodies triggers Jakarta constraints before delegation; without it
 * DTO constraints do not run here. @AuthenticationPrincipal reads the trusted user
 * from SecurityContext; removing it loses this argument resolution. Owner/seller checks
 * still belong in the application layer so non-HTTP callers cannot bypass them.
 * @ResponseStatus on creation selects 201; other methods keep their existing status.
 * Transactions are owned by services; HTTP must not hold one across a wallet call.
 */
@RestController
@RequestMapping("/api/v1/merchant/coupons")
public class MerchantCouponController {

    private final CouponService couponService;

    public MerchantCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * Merchant POST delegates validated coupon data and trusted orgIds to CouponService.
     * Membership/duplicate checks precede persistence; 201 and CouponResponse retain the
     * existing JSON contract. Checkout consumes codes separately through order placement.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CouponRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return couponService.createCoupon(user.orgIds(), request);
    }
}
