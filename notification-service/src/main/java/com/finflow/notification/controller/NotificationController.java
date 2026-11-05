package com.finflow.notification.controller;

import com.finflow.notification.dto.NotificationResponse;
import com.finflow.notification.security.AuthenticatedUser;
import com.finflow.notification.service.NotificationDispatcher;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationDispatcher dispatcher;

    public NotificationController(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /** Inbox for the JWT subject (never trust a client-supplied userId). */
    @GetMapping("/me")
    public List<NotificationResponse> mine(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return dispatcher.listForUser(user.userId());
    }
}
