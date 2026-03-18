package com.vycepay.bff.proxy;

import com.vycepay.bff.config.BffBackendProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

/**
 * Proxies /api/v1/** to backend services by path prefix. Injects X-Customer-Id from JWT when present.
 */
@RestController
@RequestMapping
public class BffProxyController {

    private static final String API_PREFIX = "/api/v1/";
    private static final String HEADER_CUSTOMER_ID = "X-Customer-Id";
    private static final Logger log = LoggerFactory.getLogger(BffProxyController.class);

    private final BffBackendProperties backend;
    private final RestTemplate restTemplate = new RestTemplate();

    public BffProxyController(BffBackendProperties backend) {
        this.backend = backend;
    }

    @RequestMapping("/api/v1/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String pathUnderApi = path.startsWith(API_PREFIX) ? path.substring(API_PREFIX.length()) : path;
        if (pathUnderApi.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("INVALID_PATH", "Path must be under /api/v1/"));
        }
        String[] segments = pathUnderApi.split("/");
        String firstSegment = segments[0];
        Map<String, String> map = backend.pathPrefixToBaseUrl();
        String baseUrl = map.get(firstSegment);
        if (baseUrl == null) {
            log.warn("BFF unknown path prefix: {}", firstSegment);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("NOT_FOUND", "Unknown API path prefix: " + firstSegment));
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
            return ResponseEntity.status(backendResponse.getStatusCode())
                    .headers(responseHeaders)
                    .body(backendResponse.getBody() != null ? backendResponse.getBody() : new byte[0]);
        } catch (HttpStatusCodeException e) {
            byte[] responseBody = e.getResponseBodyAsByteArray();
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody != null && responseBody.length > 0 ? responseBody : errorBody("BACKEND_ERROR", e.getStatusText()));
        } catch (Exception e) {
            log.error("BFF proxy error: {} {}", request.getMethod(), targetUrl, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody("BAD_GATEWAY", "Backend unreachable: " + e.getMessage()));
        }
    }

    private static byte[] errorBody(String code, String message) {
        String json = "{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
