package com.vycepay.common.kyc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.kyc.dto.FaceMatchResult;
import com.vycepay.common.kyc.dto.LivenessResult;
import com.vycepay.common.kyc.dto.OcrResult;
import com.vycepay.common.kyc.port.DocumentVerificationPort;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

/**
 * WeVipers / BBS-SDKs adapter implementing {@link DocumentVerificationPort}.
 * Base URL default: http://2.28.204.253:8080 — never expose to mobile clients.
 */
public class WeVipersKycAdapter implements DocumentVerificationPort {

    private static final Logger log = LoggerFactory.getLogger(WeVipersKycAdapter.class);

    private final String baseUrl;
    private final RestTemplate ocrClient;
    private final RestTemplate livenessClient;
    private final RestTemplate matchClient;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public WeVipersKycAdapter(String baseUrl,
                              RestTemplate ocrClient,
                              RestTemplate livenessClient,
                              RestTemplate matchClient,
                              ObjectMapper objectMapper,
                              CircuitBreaker circuitBreaker,
                              Retry retry) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.ocrClient = ocrClient;
        this.livenessClient = livenessClient;
        this.matchClient = matchClient;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public LivenessResult documentLiveness(String videoBase64) {
        Map<String, Object> body = Map.of("base64Video", Base64Media.normalize(videoBase64));
        return toLiveness(post(livenessClient, "/api/document-liveness", body));
    }

    @Override
    public LivenessResult faceLiveness(String videoBase64) {
        Map<String, Object> body = Map.of("base64Video", Base64Media.normalize(videoBase64));
        return toLiveness(post(livenessClient, "/api/face-liveness", body));
    }

    @Override
    public OcrResult extract(String documentKind, String frontImageBase64, String backImageBase64) {
        String kind = documentKind == null ? "KENYAN_ID" : documentKind.trim().toUpperCase();
        boolean both = backImageBase64 != null && !backImageBase64.isBlank();
        String path;
        Map<String, Object> body = new HashMap<>();
        switch (kind) {
            case "KENYAN_PASSPORT" -> {
                path = both ? "/api/extractKenyanPassportBoth" : "/api/extractKenyanPassport";
                if (both) {
                    body.put("base64ImageOne", Base64Media.normalize(frontImageBase64));
                    body.put("base64ImageTwo", Base64Media.normalize(backImageBase64));
                } else {
                    body.put("base64Image", Base64Media.normalize(frontImageBase64));
                }
            }
            case "PAKISTANI_CNIC" -> {
                path = both ? "/api/extractPakistaniIdCardBoth" : "/api/extractPakistaniIdCard";
                if (both) {
                    body.put("base64ImageOne", Base64Media.normalize(frontImageBase64));
                    body.put("base64ImageTwo", Base64Media.normalize(backImageBase64));
                } else {
                    body.put("base64Image", Base64Media.normalize(frontImageBase64));
                }
            }
            case "AFGHAN_ID" -> {
                path = "/api/extract";
                body.put("base64Image", Base64Media.normalize(frontImageBase64));
            }
            case "AFGHAN_LICENSE" -> {
                path = "/api/extractLicense";
                body.put("base64Image", Base64Media.normalize(frontImageBase64));
            }
            default -> {
                path = both ? "/api/extractKenyanIdCardBoth" : "/api/extractKenyanIdCard";
                if (both) {
                    body.put("base64ImageOne", Base64Media.normalize(frontImageBase64));
                    body.put("base64ImageTwo", Base64Media.normalize(backImageBase64));
                } else {
                    body.put("base64Image", Base64Media.normalize(frontImageBase64));
                }
            }
        }
        VendorResponse vr = post(ocrClient, path, body);
        Map<String, Object> payload = vr.body != null ? vr.body : Map.of();
        Map<String, Object> extracted = flattenOcr(payload);
        return new OcrResult(
                str(payload.get("detectedSide")),
                extracted,
                payload,
                str(payload.get("processingNotes")),
                vr.httpStatus,
                vr.rawJson);
    }

    @Override
    public FaceMatchResult match(String idPortraitBase64, String selfieBase64) {
        Map<String, Object> body = Map.of(
                "idCardBase64", Base64Media.normalize(idPortraitBase64),
                "selfieBase64", Base64Media.normalize(selfieBase64));
        VendorResponse vr = post(matchClient, "/api/match", body);
        Map<String, Object> payload = vr.body != null ? vr.body : Map.of();
        boolean match = Boolean.TRUE.equals(payload.get("match"));
        Double similarity = toDouble(payload.get("similarity"));
        return new FaceMatchResult(match, similarity, str(payload.get("message")), vr.httpStatus, vr.rawJson);
    }

    private LivenessResult toLiveness(VendorResponse vr) {
        Map<String, Object> payload = vr.body != null ? vr.body : Map.of();
        return new LivenessResult(
                str(payload.get("liveness_result")),
                toDouble(payload.get("confidence_score")),
                str(payload.get("kyc_recommendation")),
                str(payload.get("processing_notes") != null ? payload.get("processing_notes") : payload.get("message")),
                vr.httpStatus,
                vr.rawJson);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flattenOcr(Map<String, Object> payload) {
        Map<String, Object> out = new LinkedHashMap<>();
        Object front = payload.get("frontSide");
        if (front instanceof Map<?, ?> fm) {
            Object eng = ((Map<?, ?>) fm).get("english");
            if (eng instanceof Map<?, ?> em) {
                em.forEach((k, v) -> out.put(String.valueOf(k), v));
            }
        }
        Object dataPage = payload.get("dataPage");
        if (dataPage instanceof Map<?, ?> dm) {
            dm.forEach((k, v) -> out.put(String.valueOf(k), v));
        }
        Object extractedInfo = payload.get("extractedInfo");
        if (extractedInfo instanceof Map<?, ?> ei) {
            Object eng = ((Map<?, ?>) ei).get("english");
            if (eng instanceof Map<?, ?> em) {
                em.forEach((k, v) -> out.put(String.valueOf(k), v));
            } else {
                ei.forEach((k, v) -> out.put(String.valueOf(k), v));
            }
        }
        if (out.isEmpty()) {
            out.putAll(payload);
        }
        return out;
    }

    private VendorResponse post(RestTemplate client, String path, Map<String, Object> body) {
        Supplier<VendorResponse> call = () -> doPost(client, path, body);
        Supplier<VendorResponse> decorated = call;
        if (circuitBreaker != null) {
            decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
        }
        if (retry != null) {
            decorated = Retry.decorateSupplier(retry, decorated);
        }
        try {
            return decorated.get();
        } catch (RestClientResponseException e) {
            log.warn("WeVipers HTTP error path={} status={}", path, e.getStatusCode().value());
            Map<String, Object> errBody = parseJson(e.getResponseBodyAsString());
            return new VendorResponse(e.getStatusCode().value(), errBody, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("WeVipers call failed path={}: {}", path, e.toString());
            return new VendorResponse(503, Map.of("message", e.getMessage() != null ? e.getMessage() : "vendor error"),
                    "{\"message\":\"" + (e.getMessage() != null ? e.getMessage().replace("\"", "'") : "vendor error") + "\"}");
        }
    }

    @SuppressWarnings("unchecked")
    private VendorResponse doPost(RestTemplate client, String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = client.postForEntity(baseUrl + path, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> respBody = response.getBody() != null ? response.getBody() : Map.of();
        String raw;
        try {
            raw = objectMapper.writeValueAsString(respBody);
        } catch (Exception e) {
            raw = String.valueOf(respBody);
        }
        return new VendorResponse(response.getStatusCode().value(), respBody, raw);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (Exception e) {
            return Map.of("message", raw);
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private record VendorResponse(int httpStatus, Map<String, Object> body, String rawJson) {
    }
}
