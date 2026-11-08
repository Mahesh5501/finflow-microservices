package com.finflow.identity.controller;

import com.finflow.identity.dto.request.ChangePasswordRequest;
import com.finflow.identity.dto.request.EmailRequest;
import com.finflow.identity.dto.request.LoginRequest;
import com.finflow.identity.dto.request.RefreshTokenRequest;
import com.finflow.identity.dto.request.RegisterRequest;
import com.finflow.identity.dto.request.ResetPasswordRequest;
import com.finflow.identity.dto.request.TokenRequest;
import com.finflow.identity.dto.response.AuthenticationResponse;
import com.finflow.identity.dto.response.DevTokenResponse;
import com.finflow.identity.dto.response.RegisterResponse;
import com.finflow.identity.dto.response.UserProfileResponse;
import com.finflow.identity.security.authentication.AuthenticatedUser;
import com.finflow.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth HTTP API.
 *
 * <pre>
 * Public:  register → verify-email → login | refresh | logout | forgot/reset | resend-verification
 * Auth'd:  change-password | GET /users/me
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.email(), request.password());
    }

    /** Confirm email using token from register (or resend-verification). */
    @PostMapping("/auth/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody TokenRequest request) {
        authService.verifyEmail(request.token());
    }

    @PostMapping("/auth/resend-verification")
    public DevTokenResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        return authService.resendVerification(request.email());
    }

    @PostMapping("/auth/login")
    public AuthenticationResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/auth/refresh")
    public AuthenticationResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/auth/forgot-password")
    public DevTokenResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/auth/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.resetToken(), request.newPassword());
    }

    @PostMapping("/auth/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedUser caller = (AuthenticatedUser) authentication.getPrincipal();
        authService.changePassword(caller.userId(), request.currentPassword(), request.newPassword());
    }

    /** Profile from JWT — never trust a userId from the client body. */
    @GetMapping("/users/me")
    public UserProfileResponse me(Authentication authentication) {
        AuthenticatedUser caller = (AuthenticatedUser) authentication.getPrincipal();
        return authService.getMe(caller.userId());
    }
}
