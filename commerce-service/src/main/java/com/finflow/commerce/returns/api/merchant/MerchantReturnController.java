package com.finflow.commerce.returns.api.merchant;

import com.finflow.commerce.returns.dto.ReturnResponse;
import com.finflow.commerce.returns.application.ReturnService;
import com.finflow.commerce.security.AuthenticatedUser;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature API layer for seller approval, rejection and refunded completion. Called by Spring MVC after the SecurityFilterChain authenticates/authorizes; constructor injection supplies ReturnService.
 * Flow: bind HTTP/validate DTO -> extract caller -> application use case -> map domain inside service -> ReturnResponse -> JSON. No repository or money logic belongs here.
 * @RestController combines component discovery and response-body serialization; without it (or @Controller plus @ResponseBody) these JSON handlers are not registered.
 * @RequestMapping and verb mappings preserve the existing audience-specific routes.
 * @Valid on request bodies triggers Jakarta constraints before delegation; without it DTO constraints do not run here.
 * @AuthenticationPrincipal reads the trusted user from SecurityContext; removing it loses this argument resolution. Owner/seller checks still belong in the application layer so non-HTTP callers cannot bypass them.
 * @ResponseStatus on creation selects 201; other methods keep their existing status. Transactions are owned by services; HTTP must not hold one across a wallet call.
 */
@RestController
@RequestMapping("/api/v1/merchant/returns")
public class MerchantReturnController {

    private final ReturnService returnService;

    public MerchantReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    // Merchant POST delegates to ReturnService, which checks the original order's seller membership and transitions REQUESTED to APPROVED transactionally; invalid state conflicts.
    @PostMapping("/{returnId}/approve")
    public ReturnResponse approve(@PathVariable Long returnId, @AuthenticationPrincipal AuthenticatedUser user) {
        return returnService.approve(returnId, user.orgIds());
    }

    // Merchant POST delegates seller authorization and REQUESTED-to-REJECTED validation. The domain protects the transition; ReturnService translates failure to RETURN_STATE.
    @PostMapping("/{returnId}/reject")
    public ReturnResponse reject(@PathVariable Long returnId, @AuthenticationPrincipal AuthenticatedUser user) {
        return returnService.reject(returnId, user.orgIds());
    }

    // Merchant POST forwards the bearer header to ReturnService for approved-return refund. Refund precedes a separate restock/completion transaction; failures leave local completion undone. The current wallet owner-only API can reject this merchant token.
    @PostMapping("/{returnId}/complete")
    public ReturnResponse complete(
            @PathVariable Long returnId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return returnService.complete(returnId, user.orgIds(), authorization);
    }
}
