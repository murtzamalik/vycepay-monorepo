package com.vycepay.common.choicebank;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Choice Bank white-label wallet HMAC-SHA256 signature (generateSignature, buildWhiteLabelStringToSign).
 */
class ChoiceBankSignatureUtilTest {

    private static final String SECRET = "test-secret-key";

    @Test
    void buildWhiteLabelStringToSign_format() {
        String s = ChoiceBankSignatureUtil.buildWhiteLabelStringToSign(
                "POST",
                "/v1/wallet/create",
                "2026-02-24T10:30:45Z",
                "{\"userId\":\"U1\"}");
        assertEquals("POST\n/v1/wallet/create\n2026-02-24T10:30:45Z\n{\"userId\":\"U1\"}", s);
        assertTrue(s.contains("\n"));
        assertEquals(4, s.split("\n").length);
    }

    @Test
    void buildWhiteLabelStringToSign_emptyBody() {
        String s = ChoiceBankSignatureUtil.buildWhiteLabelStringToSign("GET", "/v1/wallet/balance", "2026-02-24T10:30:45Z", "");
        assertEquals("GET\n/v1/wallet/balance\n2026-02-24T10:30:45Z\n", s);
    }

    @Test
    void buildWhiteLabelStringToSign_nullsTreatedAsEmpty() {
        String s = ChoiceBankSignatureUtil.buildWhiteLabelStringToSign(null, null, null, null);
        assertEquals("\n\n\n", s);
    }

    @Test
    void generateSignature_base64_default() {
        String path = "/v1/wallet/create";
        String timestamp = "2026-02-24T10:30:45Z";
        String body = "{\"userId\":\"U1\"}";
        String sig = ChoiceBankSignatureUtil.generateSignature("POST", path, timestamp, body, SECRET);
        assertNotNull(sig);
        assertFalse(sig.isEmpty());
        // Base64: only [A-Za-z0-9+/=]
        assertTrue(sig.matches("^[A-Za-z0-9+/]+=*$"), "Expected Base64: " + sig);
        // Decodable
        byte[] decoded = Base64.getDecoder().decode(sig);
        assertEquals(32, decoded.length, "HMAC-SHA256 produces 32 bytes");
    }

    @Test
    void generateSignature_hex_whenRequested() {
        String path = "/v1/wallet/create";
        String timestamp = "2026-02-24T10:30:45Z";
        String body = "{\"userId\":\"U1\"}";
        String sig = ChoiceBankSignatureUtil.generateSignature("POST", path, timestamp, body, SECRET, true);
        assertNotNull(sig);
        assertTrue(sig.matches("^[0-9a-f]{64}$"), "Expected 64 hex chars: " + sig);
    }

    @Test
    void generateSignature_deterministic() {
        String path = "/v1/wallet/create";
        String timestamp = "2026-02-24T10:30:45Z";
        String body = "{\"userId\":\"U1\"}";
        String a = ChoiceBankSignatureUtil.generateSignature("POST", path, timestamp, body, SECRET);
        String b = ChoiceBankSignatureUtil.generateSignature("POST", path, timestamp, body, SECRET);
        assertEquals(a, b);
    }

    @Test
    void generateSignature_differentInputs_differentSignatures() {
        String path = "/v1/wallet/create";
        String timestamp = "2026-02-24T10:30:45Z";
        String body = "{\"userId\":\"U1\"}";

        String sig1 = ChoiceBankSignatureUtil.generateSignature("POST", path, timestamp, body, SECRET);
        String sig2 = ChoiceBankSignatureUtil.generateSignature("GET", path, timestamp, body, SECRET);
        String sig3 = ChoiceBankSignatureUtil.generateSignature("POST", "/other", timestamp, body, SECRET);
        String sig4 = ChoiceBankSignatureUtil.generateSignature("POST", path, "2026-02-24T11:00:00Z", body, SECRET);
        String sig5 = ChoiceBankSignatureUtil.generateSignature("POST", path, timestamp, "{}", SECRET);
        String sig6 = ChoiceBankSignatureUtil.generateSignature("POST", path, timestamp, body, "other-secret");

        assertNotEquals(sig1, sig2);
        assertNotEquals(sig1, sig3);
        assertNotEquals(sig1, sig4);
        assertNotEquals(sig1, sig5);
        assertNotEquals(sig1, sig6);
    }

    @Test
    void generateSignature_utf8_body() {
        String body = "{\"name\":\"café\"}";
        String sig = ChoiceBankSignatureUtil.generateSignature("POST", "/v1/wallet/create", "2026-02-24T10:30:45Z", body, SECRET);
        assertNotNull(sig);
        assertTrue(sig.matches("^[A-Za-z0-9+/]+=*$"));
    }

    @Test
    void generateSignature_emptySecret_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ChoiceBankSignatureUtil.generateSignature("POST", "/v1/wallet/create", "2026-02-24T10:30:45Z", "{}", ""));
    }
}
