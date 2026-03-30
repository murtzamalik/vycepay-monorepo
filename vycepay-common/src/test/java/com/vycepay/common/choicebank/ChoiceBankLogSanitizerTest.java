package com.vycepay.common.choicebank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChoiceBankLogSanitizerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void redactFalse_returnsUnchanged() {
        String raw = "{\"signature\":\"abc123\",\"salt\":\"xyz\"}";
        assertEquals(raw, ChoiceBankLogSanitizer.sanitizeJson(raw, false));
    }

    @Test
    void redactTrue_replacesTopLevelSignatureAndSalt() throws Exception {
        String raw = "{\"code\":\"00000\",\"signature\":\"secret\",\"salt\":\"salty\",\"data\":{\"txId\":\"t1\"}}";
        String out = ChoiceBankLogSanitizer.sanitizeJson(raw, true);
        JsonNode n = MAPPER.readTree(out);
        assertEquals("[REDACTED]", n.get("signature").asText());
        assertEquals("[REDACTED]", n.get("salt").asText());
        assertEquals("00000", n.get("code").asText());
        assertEquals("t1", n.get("data").get("txId").asText());
    }

    @Test
    void redactTrue_nestedObjects() throws Exception {
        String raw = "{\"params\":{\"signature\":\"nestedSig\"},\"signature\":\"top\"}";
        String out = ChoiceBankLogSanitizer.sanitizeJson(raw, true);
        JsonNode n = MAPPER.readTree(out);
        assertEquals("[REDACTED]", n.get("signature").asText());
        assertEquals("[REDACTED]", n.get("params").get("signature").asText());
    }

    @Test
    void blankOrNull_returnsAsIs() {
        assertEquals("", ChoiceBankLogSanitizer.sanitizeJson("", true));
        org.junit.jupiter.api.Assertions.assertNull(ChoiceBankLogSanitizer.sanitizeJson(null, true));
    }

    @Test
    void invalidJson_returnsOriginal() {
        String bad = "not json {";
        assertEquals(bad, ChoiceBankLogSanitizer.sanitizeJson(bad, true));
    }

    @Test
    void arrays_redactedInsideElements() throws Exception {
        String raw = "[{\"signature\":\"a\"},{\"salt\":\"b\"}]";
        String out = ChoiceBankLogSanitizer.sanitizeJson(raw, true);
        JsonNode arr = MAPPER.readTree(out);
        assertEquals("[REDACTED]", arr.get(0).get("signature").asText());
        assertEquals("[REDACTED]", arr.get(1).get("salt").asText());
    }
}
