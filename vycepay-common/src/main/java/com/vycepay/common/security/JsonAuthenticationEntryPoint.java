package com.vycepay.common.security;

import com.vycepay.common.exception.JsonErrorWriter;
import com.vycepay.common.exception.VyceErrorCatalog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Returns JSON error envelope for unauthenticated requests (never an empty body).
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final VyceErrorCatalog catalog;

    public JsonAuthenticationEntryPoint(VyceErrorCatalog catalog) {
        this.catalog = catalog;
    }

    public JsonAuthenticationEntryPoint() {
        this.catalog = null;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String message = catalog != null
                ? catalog.userMessage("UNAUTHORIZED")
                : "Please sign in again to continue.";
        JsonErrorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", message);
    }
}
