package com.vycepay.common.choicebank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * Example Spring Boot service that uses Choice Bank white-label wallet HMAC-SHA256 signing.
 * Requires beans: {@link org.springframework.web.client.RestTemplate}, {@link com.fasterxml.jackson.databind.ObjectMapper}.
 * Config: vycepay.choice-bank.wallet.base-url, vycepay.choice-bank.wallet.secret-key, vycepay.choice-bank.wallet.signature-hex-encoding (optional).
 */
@Service
public class ChoiceBankWalletSignatureService {

    private static final Logger log = LoggerFactory.getLogger(ChoiceBankWalletSignatureService.class);

    public static final String HEADER_TIMESTAMP = "X-Choice-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Choice-Signature";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String secretKey;
    private final boolean useHexEncoding;

    public ChoiceBankWalletSignatureService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${vycepay.choice-bank.wallet.base-url:https://api.sandbox.choicebank.com}") String baseUrl,
            @Value("${vycepay.choice-bank.wallet.secret-key:}") String secretKey,
            @Value("${vycepay.choice-bank.wallet.signature-hex-encoding:false}") boolean useHexEncoding) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "";
        this.secretKey = secretKey != null ? secretKey : "";
        this.useHexEncoding = useHexEncoding;
    }

    /**
     * Performs a signed POST to the white-label wallet API.
     *
     * @param requestPath path only, no domain (e.g. "/v1/wallet/create")
     * @param rawBody     exact JSON string to send (no pretty print, no extra spaces); empty string for no body
     * @return response body as String (or use exchange for full ResponseEntity)
     */
    public ResponseEntity<String> postSigned(String requestPath, String rawBody) {
        String timestamp = Instant.now().toString();
        String signature = ChoiceBankSignatureUtil.generateSignature(
                "POST",
                requestPath,
                timestamp,
                rawBody != null ? rawBody : "",
                secretKey,
                useHexEncoding);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_TIMESTAMP, timestamp);
        headers.set(HEADER_SIGNATURE, signature);

        String url = baseUrl + requestPath;
        HttpEntity<String> entity = new HttpEntity<>(rawBody != null ? rawBody : "", headers);

        log.debug("Calling POST {} with timestamp {}", requestPath, timestamp);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    /**
     * Example: POST JSON body from a map (compact JSON, no extra spaces).
     * Use this when you have an object and need the exact raw string for signing.
     */
    public ResponseEntity<String> postSignedJson(String requestPath, Map<String, Object> bodyMap) {
        String rawBody = bodyMap == null || bodyMap.isEmpty()
                ? ""
                : compactJson(bodyMap);
        return postSigned(requestPath, rawBody);
    }

    /**
     * GET with no body: empty string for request body in string-to-sign.
     */
    public ResponseEntity<String> getSigned(String requestPath) {
        String timestamp = Instant.now().toString();
        String signature = ChoiceBankSignatureUtil.generateSignature(
                "GET",
                requestPath,
                timestamp,
                "",
                secretKey,
                useHexEncoding);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_TIMESTAMP, timestamp);
        headers.set(HEADER_SIGNATURE, signature);

        String url = baseUrl + requestPath;
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.debug("Calling GET {} with timestamp {}", requestPath, timestamp);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    private String compactJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize body to JSON", e);
        }
    }
}
