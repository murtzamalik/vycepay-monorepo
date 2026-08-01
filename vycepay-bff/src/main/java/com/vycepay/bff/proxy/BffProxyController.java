package com.vycepay.bff.proxy;

import com.vycepay.bff.config.BffBackendProperties;
import com.vycepay.common.exception.JsonErrorWriter;
import com.vycepay.common.exception.VyceErrorCatalog;
import com.vycepay.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

/**
 * Proxies /api/v1/** to backend services by path prefix. Injects X-Customer-Id from JWT when present.
 * Always returns a customer-safe error envelope when upstream body is missing.
 */
@RestController
@RequestMapping
public class BffProxyController {

    private static final String API_PREFIX = "/api/v1/";
    private static final String HEADER_CUSTOMER_ID = "X-Customer-Id";
    private static final Logger log = LoggerFactory.getLogger(BffProxyController.class);

    private final BffBackendProperties backend;
    private final VyceErrorCatalog catalog;
    private final RestTemplate restTemplate = new RestTemplate();

    public BffProxyController(BffBackendProperties backend, VyceErrorCatalog catalog) {
        this.backend = backend;
        this.catalog = catalog;
    }

    @RequestMapping("/api/v1/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String pathUnderApi = path.startsWith(API_PREFIX) ? path.substring(API_PREFIX.length()) : path;
        if (pathUnderApi.isEmpty()) {
            return errorResponse(HttpStatus.BAD_REQUEST, "INVALID_PATH");
        }
        String[] segments = pathUnderApi.split("/");
        String firstSegment = segments[0];
        Map<String, String> map = backend.pathPrefixToBaseUrl();
        String baseUrl = map.get(firstSegment);
        if (baseUrl == null) {
            log.warn("BFF unknown path prefix: {}", firstSegment);
            return errorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND");
        }
        String targetUrl = baseUrl + API_PREFIX + pathUnderApi + (query != null && !query.isEmpty() ? "?" + query : "");

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                if ("host".equalsIgnoreCase(name) || "connection".equalsIgnoreCase(name)
                        || "content-length".equalsIgnoreCase(name)) continue;
                Enumeration<String> values = request.getHeaders(name);
                while (values.hasMoreElements()) {
                    headers.add(name, values.nextElement());
                }
            }
        }
        Object customerIdAttr = request.getAttribute(HEADER_CUSTOMER_ID);
        if (customerIdAttr != null) {
            headers.set(HEADER_CUSTOMER_ID, customerIdAttr.toString());
        }
        ensureRequestIdHeader(headers);

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(body != null ? body : new byte[0], headers);

        try {
            ResponseEntity<byte[]> backendResponse = restTemplate.exchange(
                    URI.create(targetUrl),
                    method,
                    requestEntity,
                    byte[].class);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(backendResponse.getHeaders().getContentType() != null
                    ? backendResponse.getHeaders().getContentType()
                    : MediaType.APPLICATION_JSON);
            String requestId = currentRequestId();
            responseHeaders.set(RequestIdFilter.HEADER, requestId);
            return ResponseEntity.status(backendResponse.getStatusCode())
                    .headers(responseHeaders)
                    .body(backendResponse.getBody() != null ? backendResponse.getBody() : new byte[0]);
        } catch (HttpStatusCodeException e) {
            byte[] responseBody = e.getResponseBodyAsByteArray();
            if (responseBody != null && responseBody.length > 0) {
                return ResponseEntity.status(e.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(RequestIdFilter.HEADER, currentRequestId())
                        .body(responseBody);
            }
            log.warn("BFF upstream empty error body: method={} url={} status={}",
                    request.getMethod(), targetUrl, e.getStatusCode());
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.BAD_GATEWAY;
            }
            return errorResponse(status, "UPSTREAM_ERROR");
        } catch (Exception e) {
            log.error("BFF proxy error: {} {}", request.getMethod(), targetUrl, e);
            return errorResponse(HttpStatus.BAD_GATEWAY, "BAD_GATEWAY");
        }
    }

    private ResponseEntity<byte[]> errorResponse(HttpStatus status, String code) {
        String message = catalog.userMessage(code);
        String requestId = currentRequestId();
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .header(RequestIdFilter.HEADER, requestId)
                .body(JsonErrorWriter.toBytes(code, message, requestId));
    }

    private static void ensureRequestIdHeader(HttpHeaders headers) {
        if (!headers.containsKey(RequestIdFilter.HEADER)) {
            headers.set(RequestIdFilter.HEADER, currentRequestId());
        }
    }

    private static String currentRequestId() {
        String id = MDC.get(RequestIdFilter.MDC_KEY);
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }
}
