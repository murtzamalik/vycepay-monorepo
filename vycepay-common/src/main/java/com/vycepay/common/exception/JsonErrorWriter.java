package com.vycepay.common.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Writes the standard VycePay error JSON envelope to an HTTP response (security filters, BFF).
 */
public final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    public static void write(HttpServletResponse response, int httpStatus, String code, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String json = "{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message)
                + "\",\"requestId\":\"" + escape(requestId) + "\"}";
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }

    public static byte[] toBytes(String code, String message, String requestId) {
        String id = requestId != null && !requestId.isBlank() ? requestId : UUID.randomUUID().toString();
        String json = "{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message)
                + "\",\"requestId\":\"" + escape(id) + "\"}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
