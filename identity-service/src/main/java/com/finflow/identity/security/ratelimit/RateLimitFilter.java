package com.finflow.identity.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.identity.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Applies rate limits to abuse-prone authentication endpoints
 * before the controller runs.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/resend-verification"
    );

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod()) && LIMITED.contains(path)) {
            try {
                String key = path + "|" + clientKey(request);
                if (!rateLimitService.tryConsume(key)) {
                    writeApiError(response, ApiException.tooManyRequests("Too many attempts — try again later"));
                    return;
                }
            } catch (DataAccessException ex) {
                // Fail closed (no unlimited login), but do not pretend this is a missing Bearer token.
                writeApiError(response, ApiException.serviceUnavailable("RATE_LIMIT_UNAVAILABLE", "Login rate limiting is unavailable — ensure Redis is running"));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeApiError(HttpServletResponse response, ApiException ex) throws IOException {
        response.setStatus(ex.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", ex.getCode(),
                "message", ex.getMessage()
        ));
    }

    /**
     * Uses the direct remote address by default.
     *
     * X-Forwarded-For is deliberately not trusted here because clients
     * can forge it unless a trusted reverse proxy overwrites the header.
     */
    private static String clientKey(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();

        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }
}
