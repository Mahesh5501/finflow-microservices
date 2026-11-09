
package com.finflow.commerce.controller;

import com.finflow.commerce.dto.CouponRequest;
import com.finflow.commerce.dto.CouponResponse;
import com.finflow.commerce.security.AuthenticatedUser;
import com.finflow.commerce.service.CommerceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for coupon creation.
 *
 * The actual coupon validation, organisation access checks
 * and persistence are handled by CommerceService.
 */
@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CommerceService commerceService;

    public CouponController(CommerceService commerceService) {
        this.commerceService = commerceService;
    }

    /**
     * POST /api/v1/coupons
     *
     * Creates a coupon for one of the caller's organisations.
     *
     * @Valid validates the incoming CouponRequest.
     * HTTP 201 indicates successful resource creation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(
            @Valid @RequestBody CouponRequest request,
            Authentication authentication
    ) {

        // Extract the authenticated user and their organisation memberships.
        AuthenticatedUser user =
                (AuthenticatedUser) authentication.getPrincipal();

        // Delegate coupon creation to the service.
        return commerceService.createCoupon(
                user.orgIds(),
                request
        );
    }
}
