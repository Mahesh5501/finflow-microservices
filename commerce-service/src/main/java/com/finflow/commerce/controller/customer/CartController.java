package com.finflow.commerce.controller.customer;

import com.finflow.commerce.dto.cart.CartItemRequest;
import com.finflow.commerce.dto.cart.CartResponse;
import com.finflow.commerce.dto.cart.UpdateCartItemRequest;
import com.finflow.commerce.dto.order.CheckoutRequest;
import com.finflow.commerce.dto.order.OrderResponse;
import com.finflow.commerce.service.cart.CartService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.userId());
    }

    @PostMapping("/items")
    public CartResponse addItem(
            @Valid @RequestBody CartItemRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.addItem(user.userId(), user.orgIds(), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse upsertItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.upsertItem(user.userId(), user.orgIds(), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.removeItem(user.userId(), user.orgIds(), productId);
    }

    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.clear(user.userId(), user.orgIds());
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) Long sellerOrganisationId,
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.checkout(
                user.userId(),
                user.orgIds(),
                idempotencyKey,
                authorization,
                request,
                sellerOrganisationId
        );
    }
}
