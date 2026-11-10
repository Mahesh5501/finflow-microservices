package com.finflow.commerce.controller.customer;

import com.finflow.commerce.dto.order.CreateOrderRequest;
import com.finflow.commerce.dto.order.OrderResponse;
import com.finflow.commerce.service.order.CommerceService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/orders")
public class CustomerOrderController {

    private final CommerceService commerceService;

    public CustomerOrderController(CommerceService commerceService) {
        this.commerceService = commerceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return commerceService.createOrder(
                user.userId(),
                user.orgIds(),
                idempotencyKey,
                authorization,
                request
        );
    }

    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.listCustomerOrders(user.userId());
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.getOrder(orderId, user.userId(), user.orgIds());
    }

    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(
            @PathVariable Long orderId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return commerceService.payOrder(orderId, user.userId(), user.orgIds(), authorization);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(
            @PathVariable Long orderId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return commerceService.cancelOrder(orderId, user.userId(), user.orgIds(), authorization);
    }
}
