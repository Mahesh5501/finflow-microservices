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

@RestController
@RequestMapping("/api/v1")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping("/orders/{orderId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse request(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestBody body,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return returnService.requestReturn(orderId, user.userId(), user.orgIds(), body);
    }

    @GetMapping("/orders/{orderId}/returns")
    public List<ReturnResponse> list(@PathVariable Long orderId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return returnService.listForOrder(orderId, user.orgIds());
    }

    @PostMapping("/returns/{returnId}/approve")
    public ReturnResponse approve(@PathVariable Long returnId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return returnService.approve(returnId, user.orgIds());
    }

    @PostMapping("/returns/{returnId}/reject")
    public ReturnResponse reject(@PathVariable Long returnId, Authentication authentication) {
        AuthenticatedUser user = caller(authentication);
        return returnService.reject(returnId, user.orgIds());
    }

    @PostMapping("/returns/{returnId}/complete")
    public ReturnResponse complete(
            @PathVariable Long returnId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication
    ) {
        AuthenticatedUser user = caller(authentication);
        return returnService.complete(returnId, user.orgIds(), authorization);
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
