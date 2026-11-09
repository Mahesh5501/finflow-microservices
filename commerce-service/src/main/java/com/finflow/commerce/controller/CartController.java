
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

/**
 * REST controller responsible for shopping-cart operations.
 *
 * Responsibilities:
 * 1. Receive cart-related HTTP requests.
 * 2. Extract the authenticated customer.
 * 3. Pass request data and customer information to CartService.
 * 4. Return the service response to the client.
 *
 * Business logic is kept inside CartService, not this controller.
 */
@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    private final CartService cartService; // Service that contains the actual cart and checkout business logic.

    /**
     * Constructor injection.
     * Spring injects the CartService bean when creating this controller.
     */
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * GET /api/v1/carts?organisationId=10
     * Retrieves the authenticated customer's cart for an organisation.
     */
    @GetMapping
    public CartResponse get(@RequestParam @NotNull Long organisationId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return cartService.getCart(user.userId(), user.orgIds(), organisationId); // Delegate cart retrieval to the service.
    }

    /**
     * POST /api/v1/carts/items
     * Adds an item to the customer's cart.
     * @Valid triggers validation of CartItemRequest fields.
     * @RequestBody converts the incoming JSON into the Java record.
     */
    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody CartItemRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return cartService.addItem(user.userId(), user.orgIds(), request); // Delegate item addition to the service.
    }

    /**
     * PUT /api/v1/carts/items/{productId}
     * Adds or updates the quantity of a product in the cart.
     * PUT is used here for an upsert-style operation: the item may already exist or may need to be added.
     */
    @PutMapping("/items/{productId}")
    public CartResponse upsertItem(@PathVariable Long productId, @Valid @RequestBody UpdateCartItemRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return cartService.upsertItem(user.userId(), user.orgIds(), productId, request); // Delegate item upsert to the service.
    }

    /**
     * DELETE /api/v1/carts/items/{productId}?organisationId=10
     * Removes one product from the customer's cart.
     */
    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable Long productId, @RequestParam @NotNull Long organisationId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return cartService.removeItem(user.userId(), user.orgIds(), organisationId, productId); // Delegate item removal to the service.
    }

    /**
     * DELETE /api/v1/carts?organisationId=10
     * Removes all items from the customer's cart.
     */
    @DeleteMapping
    public CartResponse clear(@RequestParam @NotNull Long organisationId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return cartService.clear(user.userId(), user.orgIds(), organisationId); // Delegate cart clearing to the service.
    }

    /**
     * POST /api/v1/carts/checkout
     * Converts the customer's cart into an order.
     * Idempotency-Key: Prevents duplicate checkout orders when a client retries a request.
     * Authorization: Passed to the service because the wallet integration may need the caller's bearer token when communicating with the wallet service.
     * CREATED: Returns HTTP 201 when the checkout operation succeeds.
     */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @Valid @RequestBody CheckoutRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return cartService.checkout(user.userId(), user.orgIds(), idempotencyKey, authorization, request); // Delegate checkout to the service.
    }

    /**
     * Extracts our custom AuthenticatedUser from Spring Security.
     * The JWT authentication flow places AuthenticatedUser inside Authentication.getPrincipal().
     * Keeping this extraction in one helper avoids repeating the same cast in every endpoint.
     */
    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
