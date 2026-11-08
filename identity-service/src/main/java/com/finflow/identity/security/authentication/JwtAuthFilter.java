package com.finflow.identity.security.authentication;

import com.finflow.identity.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7).trim();

                if (token.isEmpty()) {
                    throw new IllegalArgumentException("Bearer token is empty");
                }

                Claims claims = jwtService.parse(token);

                String subject = claims.getSubject();
                if (subject == null || subject.isBlank()) {
                    throw new IllegalArgumentException("JWT subject is missing");
                }

                Long userId = Long.valueOf(subject);
                if (userId <= 0) {
                    throw new IllegalArgumentException("JWT subject must be a positive user ID");
                }

                String email = claims.get("email", String.class);
                if (email == null || email.isBlank()) {
                    throw new IllegalArgumentException("JWT email claim is missing");
                }

                List<String> permissions = jwtService.permissions(claims);
                AuthenticatedUser caller = new AuthenticatedUser(userId, email, permissions);
                var authentication = new UsernamePasswordAuthenticationToken(
                        caller,
                        null,
                        AuthorityUtils.createAuthorityList(permissions.toArray(String[]::new)));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception ignored) {
                // Invalid token must never leave an authenticated context behind.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
