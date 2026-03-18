package com.vycepay.common.choicebank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * Live test: call Choice Bank getBankCodes with Option 1 (service) and Option 2 (util only).
 * Uses real credentials (senderId/senderKey) from env or hardcoded for test.
 * getBankCodes is a BaaS endpoint; it expects BaaS signing (signature in body). This test
 * calls it with white-label HMAC (signature in header) to show the result clearly.
 */
class ChoiceBankGetBankCodesIntegrationTest {

    private static final String BASE_URL = "https://baas-pilot.choicebankapi.com";
    private static final String PATH = "/staticData/getBankCodes";
    private static final String SENDER_ID = "VYCEIN";
    /** CTO-verified key (with hyphen); use same as RunGetBankCodes / vycepay_test.env */
    private static final String SECRET_KEY = "9x9euCAYsXxBaw0-2jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j";

    @Test
    @DisplayName("BaaS client: ChoiceBankClient getBankCodes (signature in body, requestId = senderId+UUID no hyphens)")
    void baasClient_getBankCodes_returns00000() {
        ObjectMapper objectMapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();
        ChoiceBankClient client = new ChoiceBankClient(BASE_URL, SENDER_ID, SECRET_KEY, restTemplate, objectMapper);

        ChoiceBankResponse response = client.post("staticData/getBankCodes", Map.of());

        String code = response.getCode();
        System.out.println("=== BaaS ChoiceBankClient → getBankCodes ===");
        System.out.println("Response code: " + code);
        System.out.println("Message: " + response.getMsg());
        assert "00000".equals(code) : "Expected 00000, got " + code + " – ensure requestId format is senderId+UUID(no hyphens)";
    }

    @Test
    @DisplayName("Option 2: HMAC-SHA256 util only → call getBankCodes (signature in header)")
    void option2_utilOnly_getBankCodes() {
        String method = "POST";
        String timestamp = Instant.now().toString();
        String body = "{}";

        String signature = ChoiceBankSignatureUtil.generateSignature(method, PATH, timestamp, body, SECRET_KEY, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Choice-Timestamp", timestamp);
        headers.set("X-Choice-Signature", signature);

        RestTemplate rest = new RestTemplate();
        ResponseEntity<String> response = rest.exchange(
                BASE_URL + PATH,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        int status = response.getStatusCode().value();
        String responseBody = response.getBody();
        String code = parseCode(responseBody);

        System.out.println("=== Option 2 (util only) → getBankCodes ===");
        System.out.println("HTTP Status: " + status);
        System.out.println("Response code (Choice Bank): " + code);
        System.out.println("Response body: " + (responseBody != null ? responseBody.substring(0, Math.min(300, responseBody.length())) : "") + (responseBody != null && responseBody.length() > 300 ? "..." : ""));
        System.out.println();

        // getBankCodes expects BaaS signing (signature in body), so we expect 12004 or similar when using header HMAC
        assert status == 200 : "API returned " + status;
    }

    @Test
    @DisplayName("Option 1: ChoiceBankWalletSignatureService → call getBankCodes (signature in header)")
    void option1_service_getBankCodes() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        ChoiceBankWalletSignatureService service = new ChoiceBankWalletSignatureService(
                restTemplate,
                objectMapper,
                BASE_URL,
                SECRET_KEY,
                false);

        ResponseEntity<String> response = service.postSigned(PATH, "{}");

        int status = response.getStatusCode().value();
        String responseBody = response.getBody();
        String code = parseCode(responseBody);

        System.out.println("=== Option 1 (ChoiceBankWalletSignatureService) → getBankCodes ===");
        System.out.println("HTTP Status: " + status);
        System.out.println("Response code (Choice Bank): " + code);
        System.out.println("Response body: " + (responseBody != null ? responseBody.substring(0, Math.min(300, responseBody.length())) : "") + (responseBody != null && responseBody.length() > 300 ? "..." : ""));
        System.out.println();

        assert status == 200 : "API returned " + status;
    }

    private static String parseCode(String json) {
        if (json == null) return "";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new ObjectMapper().readValue(json, Map.class);
            Object c = map.get("code");
            return c != null ? c.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
