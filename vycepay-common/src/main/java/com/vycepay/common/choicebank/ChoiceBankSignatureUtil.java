package com.vycepay.common.choicebank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Signs and verifies Choice Bank API requests/responses.
 * <ul>
 *   <li><b>BaaS API:</b> Flatten JSON to ASCII-sorted key=value string, then SHA-256 (see {@link #sign(Map)}).</li>
 *   <li><b>White-label wallet API:</b> HMAC-SHA256 over HTTP_METHOD + path + timestamp + body (see {@link #generateSignature(String, String, String, String, String, boolean)}).</li>
 * </ul>
 */
public final class ChoiceBankSignatureUtil {

    private static final Logger log = LoggerFactory.getLogger(ChoiceBankSignatureUtil.class);

    private static final String SHA_256 = "SHA-256";
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String DELIMITER = "&";
    private static final String NEWLINE = "\n";

    private ChoiceBankSignatureUtil() {
        // utility class
    }

    // ==================== White-label wallet API (HMAC-SHA256) ====================

    /**
     * Builds the string-to-sign for Choice Bank white-label wallet API.
     * Strict order: HTTP_METHOD + "\n" + REQUEST_PATH + "\n" + TIMESTAMP + "\n" + RAW_REQUEST_BODY.
     *
     * @param httpMethod  e.g. "POST", "GET"
     * @param requestPath path only, no domain (e.g. "/v1/wallet/create")
     * @param timestamp   ISO-8601 (e.g. 2026-02-24T10:30:45Z)
     * @param requestBody exact raw JSON string; empty string if no body
     * @return string to sign (UTF-8, newlines are exactly "\n")
     */
    public static String buildWhiteLabelStringToSign(String httpMethod, String requestPath,
                                                     String timestamp, String requestBody) {
        String method = httpMethod != null ? httpMethod : "";
        String path = requestPath != null ? requestPath : "";
        String ts = timestamp != null ? timestamp : "";
        String body = requestBody != null ? requestBody : "";
        return method + NEWLINE + path + NEWLINE + ts + NEWLINE + body;
    }

    /**
     * Generates HMAC-SHA256 signature for Choice Bank white-label wallet API (Base64 by default).
     *
     * @param httpMethod  e.g. "POST", "GET"
     * @param requestPath path only, no domain (e.g. "/v1/wallet/create")
     * @param timestamp   ISO-8601 (e.g. {@code Instant.now().toString()})
     * @param requestBody exact raw JSON string sent in request; empty string if no body
     * @param secretKey   HMAC secret key (UTF-8)
     * @return Base64-encoded signature
     */
    public static String generateSignature(String httpMethod, String requestPath,
                                           String timestamp, String requestBody,
                                           String secretKey) {
        return generateSignature(httpMethod, requestPath, timestamp, requestBody, secretKey, false);
    }

    /**
     * Generates HMAC-SHA256 signature for Choice Bank white-label wallet API with configurable encoding.
     *
     * @param httpMethod     e.g. "POST", "GET"
     * @param requestPath    path only, no domain (e.g. "/v1/wallet/create")
     * @param timestamp      ISO-8601 (e.g. {@code Instant.now().toString()})
     * @param requestBody    exact raw JSON string sent in request; no pretty formatting, no extra spaces; empty string if no body
     * @param secretKey      HMAC secret key (UTF-8)
     * @param useHexEncoding if true return hex string, else Base64
     * @return signature (Base64 or hex, thread-safe)
     */
    public static String generateSignature(String httpMethod, String requestPath,
                                           String timestamp, String requestBody,
                                           String secretKey, boolean useHexEncoding) {
        String stringToSign = buildWhiteLabelStringToSign(httpMethod, requestPath, timestamp, requestBody);
        byte[] keyBytes = (secretKey != null ? secretKey : "").getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, HMAC_SHA_256);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(keySpec);
            byte[] stringBytes = stringToSign.getBytes(StandardCharsets.UTF_8);
            byte[] hmacBytes = mac.doFinal(stringBytes);
            String signature = useHexEncoding ? bytesToHex(hmacBytes) : Base64.getEncoder().encodeToString(hmacBytes);
            if (log.isDebugEnabled()) {
                log.debug("ChoiceBank white-label string-to-sign:\n{}", stringToSign);
                log.debug("ChoiceBank white-label generated signature ({}): {}", useHexEncoding ? "HEX" : "Base64", signature);
            }
            return signature;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid secret key for HMAC", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }


    /**
     * Builds the string to sign from a flat key-value map.
     * Keys are sorted by raw byte (ASCII) order; joined with &amp;.
     *
     * @param flatMap Map of key to value (nested keys like params.name flattened)
     * @return Flattened string for hashing (trimmed; no leading/trailing space per Choice Bank)
     */
    public static String buildStringToSign(Map<String, String> flatMap) {
        TreeMap<String, String> sorted = new TreeMap<>(String::compareTo);
        sorted.putAll(flatMap);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) sb.append(DELIMITER);
            sb.append(e.getKey()).append("=").append(e.getValue() != null ? e.getValue() : "");
        }
        return sb.toString().trim();
    }

    /**
     * Computes SHA-256 hex of the given string.
     *
     * @param input String to hash
     * @return Hex-encoded SHA-256 hash
     * @throws IllegalStateException if SHA-256 is unavailable
     */
    public static String sha256Hex(String input) {
        if (input != null) input = input.trim();
        try {
            MessageDigest md = MessageDigest.getInstance(SHA_256);
            byte[] hash = md.digest((input != null ? input : "").getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Signs a request: build string to sign, then SHA-256.
     *
     * @param flatMap Map including salt and senderKey (senderKey removed after signing)
     * @return Signature hex string
     */
    public static String sign(Map<String, String> flatMap) {
        return sha256Hex(buildStringToSign(flatMap));
    }

    /**
     * Flattens a nested Map for signing. Nested keys use dot notation (e.g. data.accountId).
     * Per Choice Bank: same flattening used for request params and response data.
     *
     * @param flat   Target map to add flattened entries to
     * @param prefix Prefix for keys (e.g. "data")
     * @param map    Nested map to flatten
     */
    @SuppressWarnings("unchecked")
    public static void flattenNested(Map<String, String> flat, String prefix, Map<String, Object> map) {
        if (map == null) return;
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

    /**
     * Builds flat map from response fields for signature verification.
     * Excludes signature; includes data flattened when it's a Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> buildResponseFlatMap(String code, String msg, String requestId,
                                                           String sender, String locale, Long timestamp, String salt,
                                                           Object data, String senderKey) {
        Map<String, String> flat = new HashMap<>();
        flat.put("code", code != null ? code : "");
        flat.put("locale", locale != null && !locale.isBlank() ? locale : "en_KE");
        flat.put("msg", msg != null ? msg : "");
        flat.put("requestId", requestId != null ? requestId : "");
        flat.put("salt", salt != null ? salt : "");
        flat.put("sender", sender != null ? sender : "");
        flat.put("timestamp", timestamp != null ? String.valueOf(timestamp) : "");
        if (data instanceof Map) {
            flattenNested(flat, "data", (Map<String, Object>) data);
        } else if (data != null) {
            flat.put("data", String.valueOf(data));
        }
        if (senderKey != null) {
            flat.put("senderKey", senderKey);
        }
        return flat;
    }

    /**
     * Builds flat map from callback payload for signature verification.
     * Callbacks have requestId, sender, locale, timestamp, notificationType, params, salt.
     * Params are flattened under "params." prefix.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> buildCallbackFlatMap(String requestId, String sender, String locale,
                                                          Long timestamp, String notificationType,
                                                          Object params, String salt, String senderKey) {
        Map<String, String> flat = new HashMap<>();
        flat.put("locale", locale != null && !locale.isBlank() ? locale : "en_KE");
        flat.put("notificationType", notificationType != null ? notificationType : "");
        flat.put("requestId", requestId != null ? requestId : "");
        flat.put("salt", salt != null ? salt : "");
        flat.put("sender", sender != null ? sender : "");
        flat.put("timestamp", timestamp != null ? String.valueOf(timestamp) : "");
        if (params instanceof Map) {
            flattenNested(flat, "params", (Map<String, Object>) params);
        }
        if (senderKey != null) {
            flat.put("senderKey", senderKey);
        }
        return flat;
    }
}
