package com.vycepay.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Validates JWT from Authorization: Bearer header and sets X-Customer-Id for downstream.
 * On success, sets SecurityContext with externalId as principal.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTH = "Authorization";
    private static final String PREFIX_BEARER = "Bearer ";
    private static final String HEADER_CUSTOMER_ID = "X-Customer-Id";

    private final JwtValidator jwtValidator;

    public JwtAuthFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HEADER_AUTH);
        String customerIdHeader = request.getHeader(HEADER_CUSTOMER_ID);

        if (authHeader != null && authHeader.startsWith(PREFIX_BEARER)) {
            String token = authHeader.substring(PREFIX_BEARER.length());
            String externalId = jwtValidator.validateAndGetExternalId(token);
            if (externalId != null) {
                var auth = new UsernamePasswordAuthenticationToken(
                        externalId, null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                response.setHeader(HEADER_CUSTOMER_ID, externalId);
            }
        } else if (customerIdHeader != null && !customerIdHeader.isBlank()) {
            var auth = new UsernamePasswordAuthenticationToken(
                    customerIdHeader, null, Collections.emptyList());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
