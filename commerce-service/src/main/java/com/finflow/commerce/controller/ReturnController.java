
package com.finflow.commerce.controller;

import com.finflow.commerce.dto.ReturnRequestBody;
import com.finflow.commerce.dto.ReturnResponse;
import com.finflow.commerce.security.AuthenticatedUser;
import com.finflow.commerce.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
 * REST controller responsible for return-management APIs.
 *
 * Responsibilities:
 * 1. Receive HTTP requests related to returns.
 * 2. Extract the authenticated user's identity and organisation access.
 * 3. Delegate return operations to ReturnService.
 * 4. Return the resulting response DTO to the client.
 *
 * Important: This controller does not approve returns, refund money, or restock products directly. Those operations belong to the service.
 */
@RestController
@RequestMapping("/api/v1")
public class ReturnController {

    private final ReturnService returnService; // ReturnService contains return creation and management logic.

    /**
     * Constructor injection.
     * Spring injects the ReturnService bean when creating this controller.
     */
    public ReturnController(ReturnService returnService) {
        this.returnService = returnService; // Inject the ReturnService bean when creating this controller.
    }

    /**
     * POST /api/v1/orders/{orderId}/returns
     * Customer requests a return for an order.
     * @PathVariable extracts orderId from the URL.
     * @Valid validates the ReturnRequestBody before the service method is executed.
     * @RequestBody converts incoming JSON into ReturnRequestBody.
     * HTTP 201 means a new return request was created.
     */
    @PostMapping("/orders/{orderId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse request(@PathVariable Long orderId, @Valid @RequestBody ReturnRequestBody body, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return returnService.requestReturn(orderId, user.userId(), user.orgIds(), body); // Delegate return creation and customer/order validation to the service.
    }
    /**
     * GET /api/v1/orders/{orderId}/returns
     * Retrieves all return requests associated with an order.
     * Returns a List because an order may have multiple return records.
     */
    @GetMapping("/orders/{orderId}/returns")
    public List<ReturnResponse> list(@PathVariable Long orderId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return returnService.listForOrder(orderId, user.orgIds()); // Delegate return listing to the service.
    }

    /**
     * POST /api/v1/returns/{returnId}/approve
     * Approves a previously requested return.
     * The service checks the current return status before changing it.
     */
    @PostMapping("/returns/{returnId}/approve")
    public ReturnResponse approve(@PathVariable Long returnId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return returnService.approve(returnId, user.orgIds()); // Delegate return approval to the service.
    }

    /**
     * POST /api/v1/returns/{returnId}/reject
     * Rejects a return request.
     * Only a return in REQUESTED status can be rejected according to the current domain rules.
     */
    @PostMapping("/returns/{returnId}/reject")
    public ReturnResponse reject(@PathVariable Long returnId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return returnService.reject(returnId, user.orgIds()); // Delegate return rejection to the service.
    }

    /**
     * POST /api/v1/returns/{returnId}/complete
        * Completes an approved return.
     * The Authorization header is forwarded because the service may need it when communicating with the wallet service to process the refund.
     */
    @PostMapping("/returns/{returnId}/complete")
    public ReturnResponse complete(@PathVariable Long returnId, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, Authentication authentication) {
        AuthenticatedUser user = caller(authentication); // Extract the authenticated user's identity and organisation access.
        return returnService.complete(returnId, user.orgIds(), authorization); // Delegate return completion to the service.
    }

    /**
     * Common helper to extract AuthenticatedUser from Spring Security.
     * The JWT authentication flow is expected to place our custom AuthenticatedUser object inside Authentication.getPrincipal().
     */
    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
