
package com.finflow.commerce.controller;

import com.finflow.commerce.dto.CreateOrderRequest;
import com.finflow.commerce.dto.OrderResponse;
import com.finflow.commerce.security.AuthenticatedUser;
import com.finflow.commerce.service.CommerceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller responsible for order-related APIs.
 * It handles HTTP requests and delegates order business operations to CommerceService.
 * It does not directly manipulate orders or payment entities.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CommerceService commerceService; // CommerceService contains order creation and management logic.

    /**
     * Constructor injection.
     * Spring injects the CommerceService bean when creating this controller.
     */
    public OrderController(CommerceService commerceService) {
        this.commerceService = commerceService; // Inject the CommerceService bean when creating this controller.
    }

    /**
     * POST /api/v1/orders
     * Creates an order directly from the supplied order items.
     * Idempotency-Key prevents duplicate order creation on retries.
     * Authorization is forwarded for possible wallet-service calls.
     * HTTP 201 indicates successful order creation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return commerceService.createOrder(user.userId(), user.orgIds(), idempotencyKey, authorization, request); // Delegate order creation to the service.
    }

    /**
     * GET /api/v1/orders?organisationId=10&mineOnly=true
     * Lists orders belonging to an organisation.
     * mineOnly defaults to true: the caller sees only their own orders unless explicitly requesting the organisation-wide list.
     */
    @GetMapping
    public List<OrderResponse> list(@RequestParam @NotNull Long organisationId, @RequestParam(defaultValue = "true") boolean mineOnly, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return commerceService.listOrders(organisationId, user.userId(), user.orgIds(), mineOnly); // Delegate order listing to the service.
    }

    /**
     * GET /api/v1/orders/{orderId}
     * Retrieves one order.
     * Organisation membership is passed to the service so it can check access to the order.
     */
    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return commerceService.getOrder(orderId, user.orgIds()); // Delegate order retrieval to the service.
    }

    /**
     * POST /api/v1/orders/{orderId}/pay
     * Requests payment for an existing order.
     * The caller's user ID and organisation memberships are passed for ownership and access checks.
     */
    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(@PathVariable Long orderId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return commerceService.payOrder(orderId, user.userId(), user.orgIds(), authorization); // Delegate payment to the service.
    }

    /**
     * POST /api/v1/orders/{orderId}/cancel
     * Requests cancellation of an order.
     * The service decides whether cancellation is allowed and performs the necessary business operations.
     */
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return commerceService.cancelOrder(orderId, user.userId(), user.orgIds(), authorization); // Delegate cancellation to the service.
    }

    /**
     * POST /api/v1/orders/{orderId}/fulfilment/start
     * Starts fulfilment for an order.
     * The service checks whether the order can transition into the fulfilment process.
     */
    @PostMapping("/{orderId}/fulfilment/start")
    public OrderResponse startFulfilment(@PathVariable Long orderId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return commerceService.startFulfilment(orderId, user.orgIds()); // Delegate fulfilment start to the service.
    }

    /**
     * POST /api/v1/orders/{orderId}/fulfilment/complete
     * Marks the order as fulfilled.
     */
    @PostMapping("/{orderId}/fulfilment/complete")
    public OrderResponse completeFulfilment(@PathVariable Long orderId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return commerceService.markFulfilled(orderId, user.orgIds()); // Delegate fulfilment completion to the service.
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
