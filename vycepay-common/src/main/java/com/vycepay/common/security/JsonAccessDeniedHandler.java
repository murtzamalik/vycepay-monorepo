package com.vycepay.common.security;

import com.vycepay.common.exception.JsonErrorWriter;
import com.vycepay.common.exception.VyceErrorCatalog;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Returns JSON error envelope for forbidden requests (never an empty body).
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final VyceErrorCatalog catalog;

    public JsonAccessDeniedHandler(VyceErrorCatalog catalog) {
        this.catalog = catalog;
    }

    public JsonAccessDeniedHandler() {
        this.catalog = null;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String message = catalog != null
                ? catalog.userMessage("FORBIDDEN")
                : "You don't have permission to perform this action.";
        JsonErrorWriter.write(response, HttpStatus.FORBIDDEN.value(), "FORBIDDEN", message);
    }
}
