package com.vycepay.common.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.sms.port.SmsBalanceResult;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MobiWave SMS API adapter (v3). Sends plain SMS via Bearer token auth.
 * Never logs API tokens or full OTP message bodies.
 */
public class MobiWaveSmsAdapter implements SmsPort {

    private static final Logger log = LoggerFactory.getLogger(MobiWaveSmsAdapter.class);

    private final String baseUrl;
    private final String apiToken;
    private final String senderId;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public MobiWaveSmsAdapter(String baseUrl, String apiToken, String senderId,
                              RestTemplate restTemplate, ObjectMapper objectMapper,
                              CircuitBreaker circuitBreaker, Retry retry) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiToken = apiToken != null ? apiToken : "";
        this.senderId = senderId != null ? senderId : "VycePay";
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public SmsSendResult send(SmsSendRequest request) {
        if (request == null || request.recipient() == null || request.recipient().isBlank()) {
            return SmsSendResult.failed("Recipient is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            return SmsSendResult.failed("Message is required");
        }
        if (apiToken.isBlank()) {
            return SmsSendResult.failed("MobiWave API token is not configured");
        }
        String masked = KenyaPhoneNormalizer.maskRecipient(request.recipient());
        try {
            Supplier<SmsSendResult> supplier = () -> doSend(request);
            if (retry != null) {
                supplier = Retry.decorateSupplier(retry, supplier);
            }
            if (circuitBreaker != null) {
                supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
            }
            SmsSendResult result = supplier.get();
            if (result.isSent()) {
                log.info("SMS sent recipient={} providerUid={}", masked, result.providerUid());
            } else {
                log.warn("SMS not sent recipient={} status={} error={}",
                        masked, result.status(), result.errorMessage());
            }
            return result;
        } catch (Exception e) {
            log.warn("SMS send failed recipient={}: {}", masked, e.getMessage());
            return SmsSendResult.failed(e.getMessage() != null ? e.getMessage() : "SMS provider error");
        }
    }

    @Override
    public SmsBalanceResult balance() {
        if (apiToken.isBlank()) {
            return SmsBalanceResult.failed("MobiWave API token is not configured");
        }
        try {
            HttpHeaders headers = authHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/balance",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"success".equalsIgnoreCase(text(root, "status"))) {
                return SmsBalanceResult.failed(text(root, "message") != null ? text(root, "message") : "Balance lookup failed");
            }
            JsonNode data = root.get("data");
            Map<String, Object> map = new LinkedHashMap<>();
            if (data != null && data.isObject()) {
                data.fields().forEachRemaining(e -> map.put(e.getKey(), nodeValue(e.getValue())));
            }
            return SmsBalanceResult.ok(map);
        } catch (Exception e) {
            log.warn("SMS balance lookup failed: {}", e.getMessage());
            return SmsBalanceResult.failed(e.getMessage() != null ? e.getMessage() : "Balance lookup failed");
        }
    }

    private SmsSendResult doSend(SmsSendRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("recipient", request.recipient());
        body.put("sender_id", senderId);
        body.put("type", "plain");
        body.put("message", request.message());

        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/sms/send",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String status = text(root, "status");
            if ("success".equalsIgnoreCase(status)) {
                String uid = null;
                JsonNode data = root.get("data");
                if (data != null) {
                    uid = text(data, "uid");
                }
                return SmsSendResult.sent(uid);
            }
            String message = text(root, "message");
            return SmsSendResult.failed(message != null ? message : "SMS provider returned error");
        } catch (Exception e) {
            return SmsSendResult.failed("Invalid SMS provider response");
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        return headers;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://sms.mobiwave.co.ke/api/v3";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String v = node.get(field).asText();
        return v != null && !v.isBlank() ? v : null;
    }

    private static Object nodeValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isObject() || node.isArray()) {
            return node.toString();
        }
        return node.asText();
    }
}
