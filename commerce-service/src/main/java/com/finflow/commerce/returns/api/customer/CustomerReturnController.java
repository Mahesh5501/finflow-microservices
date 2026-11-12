package com.finflow.commerce.returns.api.customer;

import com.finflow.commerce.returns.dto.ReturnRequestBody;
import com.finflow.commerce.returns.dto.ReturnResponse;
import com.finflow.commerce.returns.application.ReturnService;
import com.finflow.commerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feature API layer for owner return requests and order return history. Called by Spring MVC after the SecurityFilterChain authenticates/authorizes; constructor injection supplies ReturnService.
 * Flow: bind HTTP/validate DTO -> extract caller -> application use case -> map domain inside service -> ReturnResponse -> JSON. No repository or money logic belongs here.
 * @RestController combines component discovery and response-body serialization; without it (or @Controller plus @ResponseBody) these JSON handlers are not registered.
 * @RequestMapping and verb mappings preserve the existing audience-specific routes.
 * @Valid on request bodies triggers Jakarta constraints before delegation; without it DTO constraints do not run here.
 * @AuthenticationPrincipal reads the trusted user from SecurityContext; removing it loses this argument resolution. Owner/seller checks still belong in the application layer so non-HTTP callers cannot bypass them.
 * @ResponseStatus on creation selects 201; other methods keep their existing status. Transactions are owned by services; HTTP must not hold one across a wallet call.
 */
@RestController
@RequestMapping("/api/v1/customer/orders")
public class CustomerReturnController {

    private final ReturnService returnService;

    public CustomerReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    // Customer POST validates the reason and delegates order owner/FULFILLED checks. ReturnService persists a REQUESTED row in a transaction and returns its DTO with 201; merchant membership alone does not permit requesting somebody else's return.
    @PostMapping("/{orderId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse request(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestBody body,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return returnService.requestReturn(orderId, user.userId(), user.orgIds(), body);
    }

    // Customer GET delegates the existing order-visibility check before listing return DTOs. ReturnService queries newest first inside its read-only unit of work.
    @GetMapping("/{orderId}/returns")
    public List<ReturnResponse> list(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return returnService.listForOrder(orderId, user.userId(), user.orgIds());
    }
}
