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

/**
 * Request flow step 1 for protected APIs:
 * <pre>
 *   Authorization: Bearer &lt;accessJwt&gt;
 *        → parse JWT
 *        → put {@link AuthenticatedUser} into SecurityContext
 *        → controller reads authentication.getPrincipal()
 * </pre>
 * Invalid/missing token → leave context empty; SecurityConfig then returns 401.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
                Claims claims = jwtService.parse(header.substring(7));
                Long userId = Long.valueOf(claims.getSubject());
                String email = claims.get("email", String.class);
                List<String> permissions = jwtService.permissions(claims);

                var caller = new AuthenticatedUser(userId, email, permissions);
                // Same authority strings as wallet/gateway claims (e.g. wallets:read).
                var auth = new UsernamePasswordAuthenticationToken(
                        caller,
                        null,
                        AuthorityUtils.createAuthorityList(permissions.toArray(String[]::new))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // Bad token → unauthenticated; do not fail the filter chain here.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
