package com.vycepay.common.choicebank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.choicebank.dto.ChoiceBankRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Factory for building signed Choice Bank API requests.
 * Handles salt generation, timestamp, and signature per Choice auth spec.
 */
public class ChoiceBankRequestFactory {

    private static final String LOCALE = "en_KE";
    private final String senderId;
    private final String privateKey;
    private final ObjectMapper objectMapper;

    public ChoiceBankRequestFactory(String senderId, String privateKey, ObjectMapper objectMapper) {
        this.senderId = senderId;
        this.privateKey = privateKey;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a signed Choice Bank request.
     *
     * @param requestId Unique request ID
     * @param params    Request params (will be nested under "params" key)
     * @return Signed ChoiceBankRequest ready to send
     */
    public ChoiceBankRequest build(String requestId, Map<String, Object> params) {
        long timestamp = System.currentTimeMillis();
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Map<String, String> flatMap = flattenForSigning(requestId, timestamp, salt, params);
        flatMap.put("senderKey", privateKey);

        String signature = ChoiceBankSignatureUtil.sign(flatMap);

        return ChoiceBankRequest.builder()
                .requestId(requestId)
                .sender(senderId)
                .locale(LOCALE)
                .timestamp(timestamp)
                .salt(salt)
                .signature(signature)
                .params(params)
                .build();
    }

    /**
     * Flattens request into key=value map for signing.
     * Nested keys use dot notation (e.g. params.name).
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> flattenForSigning(String requestId, long timestamp, String salt, Map<String, Object> params) {
        Map<String, String> flat = new HashMap<>();
        flat.put("locale", LOCALE);
        flat.put("requestId", requestId);
        flat.put("salt", salt);
        flat.put("sender", senderId);
        flat.put("timestamp", String.valueOf(timestamp));

        if (params != null && !params.isEmpty()) {
            flattenNested(flat, "params", params);
        } else {
            // Choice Bank: params must be in flattened string; empty = {}
            flat.put("params", "{}");
        }
        return flat;
    }

    private void flattenNested(Map<String, String> flat, String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix + "." + e.getKey();
            Object val = e.getValue();
            if (val instanceof Map) {
                flattenNested(flat, key, (Map<String, Object>) val);
            } else if (val != null) {
                flat.put(key, String.valueOf(val));
            }
        }
    }
}
