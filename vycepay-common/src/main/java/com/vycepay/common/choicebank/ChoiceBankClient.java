package com.vycepay.common.choicebank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.choicebank.dto.ChoiceBankRequest;
import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * HTTP client for Choice Bank BaaS API.
 * Signs requests, sends to configured base URL, returns parsed response.
 * When Resilience4j is on classpath, retries transient failures and opens circuit on repeated errors.
 * Optionally logs each outbound request and response (correlated by {@code requestId}).
 */
public class ChoiceBankClient {

    private static final Logger log = LoggerFactory.getLogger(ChoiceBankClient.class);

    private final String baseUrl;
    private final String senderId;
    private final ChoiceBankRequestFactory requestFactory;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final boolean loggingEnabled;
    private final boolean logBodies;
    private final boolean redactSignatures;

    /**
     * Creates client without resilience (no retry/circuit breaker).
     */
    public ChoiceBankClient(String baseUrl, String senderId, String privateKey,
                            RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(baseUrl, senderId, privateKey, restTemplate, objectMapper, null, null, true, true, true);
    }

    /**
     * Creates client with optional retry and circuit breaker for resilience.
     */
    public ChoiceBankClient(String baseUrl, String senderId, String privateKey,
                            RestTemplate restTemplate, ObjectMapper objectMapper,
                            CircuitBreaker circuitBreaker, Retry retry) {
        this(baseUrl, senderId, privateKey, restTemplate, objectMapper, circuitBreaker, retry, true, true, true);
    }

    /**
     * Full constructor including outbound logging flags (see {@code vycepay.choice-bank.logging.*}).
     */
    public ChoiceBankClient(String baseUrl, String senderId, String privateKey,
                            RestTemplate restTemplate, ObjectMapper objectMapper,
                            CircuitBreaker circuitBreaker, Retry retry,
                            boolean loggingEnabled, boolean logBodies, boolean redactSignatures) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.senderId = senderId != null ? senderId : "VYCEIN";
        this.requestFactory = new ChoiceBankRequestFactory(this.senderId, privateKey, objectMapper);
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.loggingEnabled = loggingEnabled;
        this.logBodies = logBodies;
        this.redactSignatures = redactSignatures;
    }

    /**
     * Posts a signed request to the given Choice Bank endpoint path.
     * When resilience is configured, retries on failure and uses circuit breaker.
     *
     * @param path   Endpoint path (e.g. "onboarding/v3/submitEasyOnboardingRequest")
     * @param params Request params
     * @return Parsed ChoiceBankResponse
     */
    public ChoiceBankResponse post(String path, Map<String, Object> params) {
        Supplier<ChoiceBankResponse> supplier = () -> doPost(path, params);
        if (circuitBreaker != null && retry != null) {
            return Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, supplier)).get();
        }
        if (retry != null) {
            return Retry.decorateSupplier(retry, supplier).get();
        }
        if (circuitBreaker != null) {
            return CircuitBreaker.decorateSupplier(circuitBreaker, supplier).get();
        }
        return supplier.get();
    }

    private ChoiceBankResponse doPost(String path, Map<String, Object> params) {
        String requestId = RequestIdGenerator.generate(senderId);
        ChoiceBankRequest req = requestFactory.build(requestId, params);

        String url = baseUrl + (path.startsWith("/") ? path.substring(1) : path);

        if (loggingEnabled) {
            logChoiceRequest(path, requestId, req);
        } else {
            log.debug("Choice Bank POST {} requestId={}", path, requestId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChoiceBankRequest> entity = new HttpEntity<>(req, headers);

        String json = restTemplate.postForObject(url, entity, String.class);
        ChoiceBankResponse response = parseResponse(json);
        if (response.getRequestId() == null) {
            response.setRequestId(requestId);
        }

        if (loggingEnabled) {
            logChoiceResponse(path, requestId, response, json);
        }
        return response;
    }

    private void logChoiceRequest(String path, String requestId, ChoiceBankRequest req) {
        try {
            if (logBodies) {
                String raw = objectMapper.writeValueAsString(req);
                String payload = ChoiceBankLogSanitizer.sanitizeJson(raw, redactSignatures);
                log.info("choice_baas_request choiceBankRequestId={} path={} payload={}", requestId, path, payload);
            } else {
                log.info("choice_baas_request choiceBankRequestId={} path={}", requestId, path);
            }
        } catch (JsonProcessingException e) {
            log.warn("choice_baas_request choiceBankRequestId={} path={} (failed to serialize request body)", requestId, path);
        }
    }

    private void logChoiceResponse(String path, String outboundRequestId, ChoiceBankResponse response, String rawJson) {
        String correlationId = response.getRequestId() != null ? response.getRequestId() : outboundRequestId;
        if (logBodies) {
            String payload = ChoiceBankLogSanitizer.sanitizeJson(rawJson, redactSignatures);
            log.info("choice_baas_response choiceBankRequestId={} path={} code={} msg={} payload={}",
                    correlationId, path, response.getCode(), response.getMsg(), payload);
        } else {
            log.info("choice_baas_response choiceBankRequestId={} path={} code={} msg={}",
                    correlationId, path, response.getCode(), response.getMsg());
        }
    }

    private ChoiceBankResponse parseResponse(String json) {
        try {
            return objectMapper.readValue(json, ChoiceBankResponse.class);
        } catch (Exception e) {
            String safe = ChoiceBankLogSanitizer.sanitizeJson(json, redactSignatures);
            log.error("Failed to parse Choice Bank response: {}", safe, e);
            throw new IllegalStateException("Invalid Choice Bank response", e);
        }
    }
}
