package com.vycepay.common.web;

import com.vycepay.common.logging.LoggingMdc;
import com.vycepay.common.logging.LoggingMdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Ensures every request has a correlation {@code requestId} in MDC and response header.
 * Accepts inbound {@code X-Request-Id} when present.
 * Also copies {@code X-Customer-Id} into MDC when present (BFF injects after JWT).
 * Registered via {@link com.vycepay.common.config.WebConfig} / {@link com.vycepay.common.config.VyceErrorAutoConfiguration}.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String CUSTOMER_HEADER = "X-Customer-Id";
    public static final String MDC_KEY = LoggingMdcKeys.REQUEST_ID;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);

        String customerId = request.getHeader(CUSTOMER_HEADER);
        if (customerId != null && !customerId.isBlank()) {
            LoggingMdc.put(LoggingMdcKeys.CUSTOMER_ID, customerId);
        }
        LoggingMdc.put(LoggingMdcKeys.HTTP_METHOD, request.getMethod());
        LoggingMdc.put(LoggingMdcKeys.URL_PATH, request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            LoggingMdc.clearEventFields();
            LoggingMdc.clearCustomerAndDomainIds();
            MDC.remove(MDC_KEY);
            MDC.remove(LoggingMdcKeys.HTTP_METHOD);
            MDC.remove(LoggingMdcKeys.URL_PATH);
        }
    }
}
