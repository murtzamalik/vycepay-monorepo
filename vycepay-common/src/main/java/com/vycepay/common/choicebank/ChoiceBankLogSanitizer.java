package com.vycepay.common.choicebank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

/**
 * Redacts sensitive fields from Choice Bank JSON for logging (signature, salt).
 * Recursively walks objects and arrays so nested envelopes are covered.
 */
public final class ChoiceBankLogSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ChoiceBankLogSanitizer() {
    }

    /**
     * Returns JSON safe for logs: when {@code redact} is false, returns input unchanged.
     * On parse failure, returns the original string.
     */
    public static String sanitizeJson(String json, boolean redact) {
        if (json == null || json.isBlank()) {
            return json;
        }
        if (!redact) {
            return json;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            redactRecursive(root);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    private static void redactRecursive(JsonNode node) {
        if (node == null || !node.isObject()) {
            if (node != null && node.isArray()) {
                for (JsonNode child : node) {
                    redactRecursive(child);
                }
            }
            return;
        }
        ObjectNode obj = (ObjectNode) node;
        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> e = fields.next();
            String name = e.getKey();
            JsonNode val = e.getValue();
            if ("signature".equals(name) || "salt".equals(name)) {
                obj.put(name, REDACTED);
            } else {
                redactRecursive(val);
            }
        }
    }
}
