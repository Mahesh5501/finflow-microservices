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

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CommerceService commerceService;

    public OrderController(CommerceService commerceService) {
        this.commerceService = commerceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return commerceService.createOrder(
                user.userId(), user.orgIds(), idempotencyKey, authorization, request
        );
    }

    @GetMapping
    public List<OrderResponse> list(
            @RequestParam @NotNull Long organisationId,
            @RequestParam(defaultValue = "true") boolean mineOnly,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return commerceService.listOrders(organisationId, user.userId(), user.orgIds(), mineOnly);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return commerceService.getOrder(orderId, user.orgIds());
    }

    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(
            @PathVariable Long orderId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return commerceService.payOrder(orderId, user.userId(), user.orgIds(), authorization);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(
            @PathVariable Long orderId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return commerceService.cancelOrder(orderId, user.userId(), user.orgIds(), authorization);
    }

    @PostMapping("/{orderId}/fulfilment/start")
    public OrderResponse startFulfilment(@PathVariable Long orderId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return commerceService.startFulfilment(orderId, user.orgIds());
    }

    @PostMapping("/{orderId}/fulfilment/complete")
    public OrderResponse completeFulfilment(@PathVariable Long orderId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return commerceService.markFulfilled(orderId, user.orgIds());
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
