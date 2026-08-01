package com.vycepay.callback.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Requires {@code X-Internal-Api-Key} for {@code /internal/**} routes.
 * Registered via {@link SecurityConfig} FilterRegistrationBean (not as a servlet {@code @Component}).
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private final String apiKey;

    public InternalApiKeyFilter(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (apiKey.isBlank()) {
            unauthorized(response, "Internal API key is not configured");
            return;
        }
        String provided = request.getHeader("X-Internal-Api-Key");
        if (provided == null || !constantTimeEquals(apiKey, provided)) {
            unauthorized(response, "Invalid or missing internal API key");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(
                ("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
