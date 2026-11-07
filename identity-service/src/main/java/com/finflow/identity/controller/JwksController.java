package com.finflow.identity.controller;

import com.finflow.identity.security.jwt.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public JWKS endpoint — other microservices verify RS256 access tokens without a shared secret. */
@RestController
public class JwksController {

    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwtService.jwks();
    }
}
