package com.finflow.commerce.controller.merchant;

import com.finflow.commerce.dto.returns.ReturnResponse;
import com.finflow.commerce.service.returns.ReturnService;
import com.finflow.commerce.security.AuthenticatedUser;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/returns")
public class MerchantReturnController {

    private final ReturnService returnService;

    public MerchantReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping("/{returnId}/approve")
    public ReturnResponse approve(@PathVariable Long returnId, @AuthenticationPrincipal AuthenticatedUser user) {
        return returnService.approve(returnId, user.orgIds());
    }

    @PostMapping("/{returnId}/reject")
    public ReturnResponse reject(@PathVariable Long returnId, @AuthenticationPrincipal AuthenticatedUser user) {
        return returnService.reject(returnId, user.orgIds());
    }

    @PostMapping("/{returnId}/complete")
    public ReturnResponse complete(
            @PathVariable Long returnId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return returnService.complete(returnId, user.orgIds(), authorization);
    }
}
