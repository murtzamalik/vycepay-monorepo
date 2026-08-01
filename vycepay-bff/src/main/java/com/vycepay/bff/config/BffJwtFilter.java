package com.vycepay.bff.config;

import com.vycepay.common.exception.JsonErrorWriter;
import com.vycepay.common.exception.VyceErrorCatalog;
import com.vycepay.common.security.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * BFF JWT filter: public auth paths are signup OTP, PIN login, device/migrate OTP, forgot-PIN.
 * All other /api/v1/** paths require Authorization: Bearer token; extracts externalId for proxy.
 */
public class BffJwtFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTH = "Authorization";
    private static final String PREFIX_BEARER = "Bearer ";
    private static final String ATTR_CUSTOMER_ID = "X-Customer-Id";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "POST:/api/v1/auth/register",
            "POST:/api/v1/auth/login",
            "POST:/api/v1/auth/verify-otp",
            "POST:/api/v1/auth/verify-device-otp",
            "POST:/api/v1/auth/verify-migrate-otp",
            "POST:/api/v1/auth/forgot-pin/request",
            "POST:/api/v1/auth/forgot-pin/confirm"
    );

    private final JwtValidator jwtValidator;
    private final VyceErrorCatalog catalog;

    public BffJwtFilter(JwtValidator jwtValidator, VyceErrorCatalog catalog) {
        this.jwtValidator = jwtValidator;
        this.catalog = catalog;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String key = request.getMethod() + ":" + path;
        if (PUBLIC_PATHS.contains(key)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!path.startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader(HEADER_AUTH);
        if (authHeader == null || !authHeader.startsWith(PREFIX_BEARER)) {
            JsonErrorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                    catalog.userMessage("UNAUTHORIZED"));
            return;
        }
        String token = authHeader.substring(PREFIX_BEARER.length()).trim();
        String externalId = jwtValidator.validateAndGetExternalId(token);
        if (externalId == null) {
            JsonErrorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                    catalog.userMessage("UNAUTHORIZED"));
            return;
        }
        request.setAttribute(ATTR_CUSTOMER_ID, externalId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(externalId, null, Collections.emptyList()));
        filterChain.doFilter(request, response);
    }
}
