package com.finflow.commerce.order.api.merchant;

import com.finflow.commerce.order.dto.OrderResponse;
import com.finflow.commerce.order.application.CommerceService;
import com.finflow.commerce.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Organisation order operations (fulfilment, org-wide list).
 *
 * Feature API layer for seller-scoped order lists and fulfilment transitions. Called by Spring MVC after the
 * SecurityFilterChain authenticates/authorizes; constructor injection supplies CommerceService.
 * Flow: bind HTTP/validate DTO -> extract caller -> application use case -> map domain
 * inside service -> OrderResponse -> JSON. No repository or money logic belongs here.
 *
 * @RestController combines component discovery and response-body serialization;
 * without it (or @Controller plus @ResponseBody) these JSON handlers are not registered.
 * @RequestMapping and verb mappings preserve the existing audience-specific routes.
 * @Valid on request bodies triggers Jakarta constraints before delegation; without it
 * DTO constraints do not run here. @AuthenticationPrincipal reads the trusted user
 * from SecurityContext; removing it loses this argument resolution. Owner/seller checks
 * still belong in the application layer so non-HTTP callers cannot bypass them.
 * @ResponseStatus on creation selects 201; other methods keep their existing status.
 * Transactions are owned by services; HTTP must not hold one across a wallet call.
 */
@RestController
@RequestMapping("/api/v1/merchant/orders")
public class MerchantOrderController {

    private final CommerceService commerceService;

    public MerchantOrderController(CommerceService commerceService) {
        this.commerceService = commerceService;
    }

    /**
     * Merchant GET passes organisationId and the existing mineOnly selector to the service.
     * Membership is checked before querying seller orders; a query parameter cannot grant it.
     */
    @GetMapping
    public List<OrderResponse> list(
            @RequestParam(defaultValue = "false") boolean mineOnly,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return commerceService.listSellerOrders(user.userId(), user.orgIds(), mineOnly);
    }

    /**
     * Merchant GET delegates existing owner-or-seller visibility and DTO mapping.
     * The service owns the read transaction; missing/inaccessible orders retain 404/403.
     */
    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.getOrder(orderId, user.userId(), user.orgIds());
    }

    /**
     * Merchant POST asks the service to move a same-seller CONFIRMED order to
     * FULFILMENT_PENDING in one transaction. Invalid state returns FULFIL_NOT_ALLOWED.
     */
    @PostMapping("/{orderId}/fulfilment/start")
    public OrderResponse startFulfilment(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.startFulfilment(orderId, user.orgIds());
    }

    /**
     * Merchant POST completes a same-seller CONFIRMED/FULFILMENT_PENDING order.
     * The service commits FULFILLED and currently publishes the event inside that transaction;
     * this endpoint does not call a shipping carrier or guarantee durable event delivery.
     */
    @PostMapping("/{orderId}/fulfilment/complete")
    public OrderResponse completeFulfilment(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.markFulfilled(orderId, user.orgIds());
    }
}
