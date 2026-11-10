package com.finflow.commerce.controller.customer;

import com.finflow.commerce.dto.returns.ReturnRequestBody;
import com.finflow.commerce.dto.returns.ReturnResponse;
import com.finflow.commerce.service.returns.ReturnService;
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

@RestController
@RequestMapping("/api/v1/customer/orders")
public class CustomerReturnController {

    private final ReturnService returnService;

    public CustomerReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping("/{orderId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse request(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestBody body,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return returnService.requestReturn(orderId, user.userId(), user.orgIds(), body);
    }

    @GetMapping("/{orderId}/returns")
    public List<ReturnResponse> list(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return returnService.listForOrder(orderId, user.userId(), user.orgIds());
    }
}
