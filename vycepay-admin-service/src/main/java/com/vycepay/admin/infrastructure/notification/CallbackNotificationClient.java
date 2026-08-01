package com.vycepay.admin.infrastructure.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.admin.config.AdminProperties;
import com.vycepay.common.exception.BusinessException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for callback-service internal notification compose/resend APIs.
 */
@Component
public class CallbackNotificationClient {

    private final RestTemplate restTemplate;
    private final AdminProperties properties;
    private final ObjectMapper objectMapper;

    public CallbackNotificationClient(RestTemplateBuilder builder,
                                      AdminProperties properties,
                                      ObjectMapper objectMapper) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> compose(List<Long> customerIds, String title, String body,
                                       Map<String, String> data, Long adminId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerIds", customerIds);
        payload.put("title", title);
        payload.put("body", body);
        if (data != null) {
            payload.put("data", data);
        }
        return exchange(HttpMethod.POST, "/internal/v1/notifications/compose", payload, adminId);
    }

    public Map<String, Object> resend(Long notificationId, Long adminId) {
        return exchange(HttpMethod.POST, "/internal/v1/notifications/" + notificationId + "/resend", null, adminId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchange(HttpMethod method, String path, Object body, Long adminId) {
        String base = properties.getCallbackBaseUrl();
        if (base == null || base.isBlank()) {
            throw new BusinessException("CALLBACK_NOT_CONFIGURED",
                    "Callback service URL is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        String apiKey = properties.getInternalApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("INTERNAL_KEY_NOT_CONFIGURED",
                    "Internal API key is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        String url = base.endsWith("/") ? base.substring(0, base.length() - 1) + path : base + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Key", apiKey);
        if (adminId != null) {
            headers.set("X-Admin-Id", String.valueOf(adminId));
        }
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, method, new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> envelope = response.getBody();
            if (envelope != null && envelope.get("data") instanceof Map<?, ?> data) {
                return (Map<String, Object>) data;
            }
            return envelope != null ? envelope : Map.of();
        } catch (HttpStatusCodeException e) {
            throw mapUpstream(e);
        } catch (Exception e) {
            throw new BusinessException("CALLBACK_UNAVAILABLE",
                    "Notification service unavailable", HttpStatus.BAD_GATEWAY);
        }
    }

    private BusinessException mapUpstream(HttpStatusCodeException e) {
        try {
            JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());
            String code = root.path("code").asText("NOTIFICATION_ERROR");
            String message = root.path("message").asText("Notification operation failed");
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.BAD_GATEWAY;
            }
            return new BusinessException(code, message, status);
        } catch (Exception parseError) {
            return new BusinessException("NOTIFICATION_ERROR",
                    "Notification operation failed", HttpStatus.BAD_GATEWAY);
        }
    }
}
