package com.finflow.commerce.controller;

import com.finflow.commerce.dto.CartItemRequest;
import com.finflow.commerce.dto.CartResponse;
import com.finflow.commerce.dto.CheckoutRequest;
import com.finflow.commerce.dto.OrderResponse;
import com.finflow.commerce.dto.UpdateCartItemRequest;
import com.finflow.commerce.security.AuthenticatedUser;
import com.finflow.commerce.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@RequestParam @NotNull Long organisationId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return cartService.getCart(user.userId(), user.orgIds(), organisationId);
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody CartItemRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return cartService.addItem(user.userId(), user.orgIds(), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse upsertItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return cartService.upsertItem(user.userId(), user.orgIds(), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @PathVariable Long productId,
            @RequestParam @NotNull Long organisationId,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return cartService.removeItem(user.userId(), user.orgIds(), organisationId, productId);
    }

    @DeleteMapping
    public CartResponse clear(@RequestParam @NotNull Long organisationId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return cartService.clear(user.userId(), user.orgIds(), organisationId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return cartService.checkout(user.userId(), user.orgIds(), idempotencyKey, authorization, request);
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
