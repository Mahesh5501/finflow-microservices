package com.finflow.commerce.order.api.customer;

import com.finflow.commerce.order.dto.CreateOrderRequest;
import com.finflow.commerce.order.dto.OrderResponse;
import com.finflow.commerce.order.application.CommerceService;
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

/**
 * Feature API layer for customer order creation, history, payment retry and cancellation. Called by Spring MVC after the
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
@RequestMapping("/api/v1/customer/orders")
public class CustomerOrderController {

    private final CommerceService commerceService;

    public CustomerOrderController(CommerceService commerceService) {
        this.commerceService = commerceService;
    }

    /**
     * Customer POST binds the key, optional bearer header and validated order lines.
     * CommerceService infers/checks one seller, commits placement and calls settlement
     * outside that transaction. Owner/payload key conflicts do not create a second order.
     */
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

    /**
     * Customer GET lists orders by principal user ID across sellers through CommerceService.
     * The application service maps items inside its read-only transaction.
     */
    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.listCustomerOrders(user.userId());
    }

    /**
     * Customer GET delegates visibility to CommerceService: the order owner or seller staff
     * may read under the existing contract. Unknown orders return ORDER_NOT_FOUND.
     */
    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return commerceService.getOrder(orderId, user.userId(), user.orgIds());
    }

    /**
     * Customer retry forwards bearer authentication to settlement through CommerceService.
     * Only the order owner may pay, and only pending/action-required states are allowed;
     * validation precedes the external call. Settlement keeps HTTP outside its local update.
     */
    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(
            @PathVariable Long orderId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return commerceService.payOrder(orderId, user.userId(), user.orgIds(), authorization);
    }

    /**
     * Customer cancellation delegates ownership/state checks to CommerceService.
     * A confirmed order refunds before the cancellation transaction restores stock;
     * REFUND_FAILED stops local finalization rather than pretending the refund succeeded.
     */
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(
            @PathVariable Long orderId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return commerceService.cancelOrder(orderId, user.userId(), user.orgIds(), authorization);
    }
}
