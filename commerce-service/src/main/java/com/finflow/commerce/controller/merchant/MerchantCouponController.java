package com.finflow.commerce.controller.merchant;

import com.finflow.commerce.dto.coupon.CouponRequest;
import com.finflow.commerce.dto.coupon.CouponResponse;
import com.finflow.commerce.service.order.CommerceService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/coupons")
public class MerchantCouponController {

    private final CommerceService commerceService;

    public MerchantCouponController(CommerceService commerceService) {
        this.commerceService = commerceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CouponRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.createCoupon(user.orgIds(), request);
    }
}
