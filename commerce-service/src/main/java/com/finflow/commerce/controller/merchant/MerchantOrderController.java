package com.finflow.commerce.controller.merchant;

import com.finflow.commerce.dto.order.OrderResponse;
import com.finflow.commerce.service.order.CommerceService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Organisation order operations (fulfilment, org-wide list). */
@RestController
@RequestMapping("/api/v1/merchant/orders")
public class MerchantOrderController {

    private final CommerceService commerceService;

    public MerchantOrderController(CommerceService commerceService) {
        this.commerceService = commerceService;
    }

    @GetMapping
    public List<OrderResponse> list(
            @RequestParam @NotNull Long organisationId,
            @RequestParam(defaultValue = "false") boolean mineOnly,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return commerceService.listOrders(organisationId, user.userId(), user.orgIds(), mineOnly);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.getOrder(orderId, user.userId(), user.orgIds());
    }

    @PostMapping("/{orderId}/fulfilment/start")
    public OrderResponse startFulfilment(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.startFulfilment(orderId, user.orgIds());
    }

    @PostMapping("/{orderId}/fulfilment/complete")
    public OrderResponse completeFulfilment(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.markFulfilled(orderId, user.orgIds());
    }
}
