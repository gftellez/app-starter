package com.example.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * One shared token on every {@code /api/**} call.
 *
 * This is a door lock, not sign-in: it says the caller knows the secret, not who they are.
 * Anything that needs to know <em>who</em> — per-user data, an audit trail, resolving which
 * tenant a request belongs to — needs real authentication first. See {@link
 * com.example.app.tenant.TenantContext}.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Value("${app.auth.token}")
    private String authToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        if (tokenMatches(request.getHeader("X-Auth-Token"))) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
        }
    }

    /**
     * Constant-time comparison: a plain {@code equals} returns faster the earlier the first
     * wrong byte is, which is enough to guess a token one character at a time.
     */
    private boolean tokenMatches(String header) {
        if (header == null || authToken == null || authToken.isBlank()) return false;
        return MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8),
                authToken.getBytes(StandardCharsets.UTF_8));
    }
}
