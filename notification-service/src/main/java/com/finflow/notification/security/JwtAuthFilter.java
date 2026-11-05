package com.finflow.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
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
import java.security.Key;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwksKeyProvider jwksKeyProvider;

    public JwtAuthFilter(JwksKeyProvider jwksKeyProvider) {
        this.jwksKeyProvider = jwksKeyProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String raw = header.substring(7);
                Claims claims = Jwts.parser()
                        .keyLocator(new LocatorAdapter<Key>() {
                            @Override
                            protected Key locate(JwsHeader header) {
                                return jwksKeyProvider.getKey(header.getKeyId());
                            }
                        })
                        .build()
                        .parseSignedClaims(raw)
                        .getPayload();

                Long userId = Long.valueOf(claims.getSubject());
                String email = claims.get("email", String.class);
                List<String> permissions = stringList(claims.get("permissions"));
                List<Long> orgIds = longList(claims.get("orgIds"));
                var principal = new AuthenticatedUser(userId, email, orgIds, permissions);
                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        AuthorityUtils.createAuthorityList(permissions.toArray(String[]::new))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private static List<String> stringList(Object raw) {
        if (raw instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static List<Long> longList(Object raw) {
        if (raw instanceof Collection<?> c) {
            return c.stream().map(v -> Long.valueOf(String.valueOf(v))).toList();
        }
        return List.of();
    }
}
