package com.finflow.commerce.cart.api.customer;

import com.finflow.commerce.cart.dto.CartItemRequest;
import com.finflow.commerce.cart.dto.CartResponse;
import com.finflow.commerce.cart.dto.UpdateCartItemRequest;
import com.finflow.commerce.order.dto.CheckoutRequest;
import com.finflow.commerce.order.dto.OrderResponse;
import com.finflow.commerce.cart.application.CartService;
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

/**
 * Feature API layer for customer cart CRUD and one-seller checkout. Called by Spring MVC after the SecurityFilterChain authenticates/authorizes; constructor injection supplies CartService.
 * Flow: bind HTTP/validate DTO -> extract caller -> application use case -> map domain inside service -> CartResponse or OrderResponse -> JSON. No repository or money logic belongs here.
 *
 * @RestController combines component discovery and response-body serialization;
 * without it (or @Controller plus @ResponseBody) these JSON handlers are not registered.
 * @RequestMapping and verb mappings preserve the existing audience-specific routes.
 * @Valid on request bodies triggers Jakarta constraints before delegation; without it DTO constraints do not run here. 
 * @AuthenticationPrincipal reads the trusted user from SecurityContext; removing it loses this argument resolution. 
 * Owner/seller checks still belong in the application layer so non-HTTP callers cannot bypass them.
 * @ResponseStatus on creation selects 201; other methods keep their existing status.
 * Transactions are owned by services; HTTP must not hold one across a wallet call.
 */
@RestController
@RequestMapping("/api/v1/customer/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Customer GET uses only the authenticated user ID to load the merged seller buckets.
     * CartService maps current product prices inside a read-only transaction.
     */
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.userId());
    }

    /**
     * Customer POST validates positive quantity, then delegates with the authenticated owner.
     * CartService derives the seller from Product, checks activity/stock and increments a line.
     */
    @PostMapping("/items")
    public CartResponse addItem(
            @Valid @RequestBody CartItemRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.addItem(user.userId(), user.orgIds(), request);
    }

    /**
     * Customer PUT binds the product ID and validated replacement quantity; zero removes.
     * CartService checks product/stock and updates only this owner's seller bucket.
     */
    @PutMapping("/items/{productId}")
    public CartResponse upsertItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.upsertItem(user.userId(), user.orgIds(), productId, request);
    }

    /**
     * Customer DELETE delegates owner and product ID; CartService resolves the stored seller
     * and removes that owner's line. Missing carts remain a no-op, missing products return 404.
     */
    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.removeItem(user.userId(), user.orgIds(), productId);
    }

    /**
     * Customer DELETE clears this principal's seller buckets through CartService.
     * The HTTP caller cannot supply another customer ID to this operation.
     */
    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.clear(user.userId(), user.orgIds());
    }

    /**
     * Customer checkout forwards the stable key, optional seller selector, address and bearer
     * header to CartService. Snapshot and clear use separate transactions around the order
     * workflow; empty/multi-seller selection errors happen there, and 201 preserves the
     * existing response contract even when payment returns declined or uncertain.
     */
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
