package com.finflow.commerce.cart.api.customer;

import com.finflow.commerce.cart.dto.CartItemRequest;
import com.finflow.commerce.cart.dto.CartResponse;
import com.finflow.commerce.cart.dto.UpdateCartItemRequest;
import com.finflow.commerce.order.dto.CheckoutRequest;
import com.finflow.commerce.order.dto.CheckoutResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature API layer for customer cart CRUD and marketplace checkout. Called by Spring MVC after
 * the SecurityFilterChain authenticates/authorizes; constructor injection supplies CartService.
 *
 * <p>Flow: bind HTTP/validate DTO → extract caller → application use case → map domain inside
 * service → {@link CartResponse} or {@link CheckoutResponse} → JSON. No repository or money
 * logic belongs here.</p>
 *
 * <p>Marketplace rules: shoppers identify by login only — no {@code organisationId} or
 * {@code sellerId} on customer cart requests. Seller ownership is derived from each
 * {@link com.finflow.commerce.catalogue.domain.Product} when lines are added.</p>
 *
 * <p>{@code @RestController} combines component discovery and response-body serialization;
 * without it (or {@code @Controller} plus {@code @ResponseBody}) these JSON handlers are not
 * registered. {@code @RequestMapping} and verb mappings preserve the customer cart routes.
 * {@code @Valid} on request bodies triggers Jakarta constraints before delegation.
 * {@code @AuthenticationPrincipal} reads the trusted user from SecurityContext.
 * Owner/seller checks still belong in the application layer so non-HTTP callers cannot bypass them.
 * {@code @ResponseStatus} on checkout selects 201. Transactions are owned by services; HTTP must not
 * hold one across a wallet call.</p>
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
        return cartService.addItem(user.userId(), request);
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
        return cartService.upsertItem(user.userId(), productId, request);
    }

    /**
     * Customer DELETE delegates owner and product ID; CartService resolves the stored seller
     * and removes that owner's line. Missing carts remain a no-op; missing products return 404.
     */
    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.removeItem(user.userId(), productId);
    }

    /**
     * Customer DELETE clears every seller bucket for this authenticated user.
     * The HTTP caller cannot supply another customer ID to this operation.
     */
    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.clear(user.userId());
    }

    /**
     * Customer checkout forwards the stable idempotency key, shipping address and optional bearer
     * header to CartService. One {@link CheckoutResponse} is returned with one order per nonempty
     * seller bucket (multi-seller cart in a single call). Snapshot, payment and per-seller cart
     * clear use separate transactions around the order workflow; empty cart fails before ordering.
     * Do not wrap the controller in a transaction — wallet calls must happen after placement commits.
     */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse checkout(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return cartService.checkout(
                user.userId(),
                idempotencyKey,
                authorization,
                request
        );
    }
}
