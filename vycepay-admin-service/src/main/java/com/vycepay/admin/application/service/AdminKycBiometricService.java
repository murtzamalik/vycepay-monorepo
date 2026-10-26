package com.vycepay.admin.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


/**
 * Enriches admin KYC payloads with biometric session + attempts.
 */
@Service
public class AdminKycBiometricService {

    private final JdbcTemplate jdbcTemplate;
    private final AdminSecurityContext securityContext;

    public AdminKycBiometricService(JdbcTemplate jdbcTemplate, AdminSecurityContext securityContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.securityContext = securityContext;
    }

    public void enrichKycDetail(Map<String, Object> row) {
        Long kycId = toLong(row.get("id"));
        Long biometricSessionPk = toLong(row.get("biometric_session_id"));
        boolean docs = securityContext.hasPermission("kyc:view_documents");

        Map<String, Object> documents = new LinkedHashMap<>();
        String idFront = str(row.remove("id_front_url"));
        String idBack = str(row.remove("id_back_url"));
        String idPortrait = str(row.remove("id_portrait_url"));
        String selfie = str(row.remove("selfie_url"));
        if (docs) {
            documents.put("idFrontUrl", docUrl(kycId, "id-front", idFront));
            documents.put("idBackUrl", docUrl(kycId, "id-back", idBack));
            documents.put("idPortraitUrl", docUrl(kycId, "id-portrait", idPortrait));
            documents.put("selfieUrl", docUrl(kycId, "selfie", selfie));
        } else {
            documents = null;
        }
        row.put("documents", documents);

        // camelCase aliases commonly used by portal
        row.putIfAbsent("choiceOnboardingRequestId", row.get("choice_onboarding_request_id"));
        row.putIfAbsent("createdAt", row.get("created_at"));
        row.putIfAbsent("idType", row.get("id_type"));

        if (biometricSessionPk == null) {
            row.put("biometric", null);
            return;
        }
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(
                "SELECT * FROM kyc_biometric_session WHERE id=?", biometricSessionPk);
        if (sessions.isEmpty()) {
            row.put("biometric", null);
            return;
        }
        Map<String, Object> s = sessions.get(0);
        Map<String, Object> biometric = new LinkedHashMap<>();
        biometric.put("sessionId", s.get("public_id"));
        biometric.put("status", s.get("status"));
        biometric.put("currentStep", s.get("current_step"));
        biometric.put("faceMatch", s.get("face_match"));
        biometric.put("faceSimilarity", s.get("face_similarity"));
        biometric.put("failReasonCode", s.get("fail_reason_code"));
        biometric.put("failReasonMsg", s.get("fail_reason_msg"));
        biometric.put("expiresAt", s.get("expires_at"));

        if (docs) {
            // fill docs from session if verification URLs empty
            if (documents != null) {
                if (documents.get("idFrontUrl") == null) {
                    documents.put("idFrontUrl", docUrl(kycId, "id-front", str(s.get("id_front_url"))));
                }
                if (documents.get("idBackUrl") == null) {
                    documents.put("idBackUrl", docUrl(kycId, "id-back", str(s.get("id_back_url"))));
                }
                if (documents.get("idPortraitUrl") == null) {
                    documents.put("idPortraitUrl", docUrl(kycId, "id-portrait", str(s.get("id_portrait_url"))));
                }
                if (documents.get("selfieUrl") == null) {
                    documents.put("selfieUrl", docUrl(kycId, "selfie", str(s.get("selfie_url"))));
                }
            }
        }

        List<Map<String, Object>> attempts = jdbcTemplate.queryForList(
                "SELECT public_id, step, attempt_no, outcome, liveness_result, confidence_score, match_result, "
                        + "similarity, fail_code, fail_message, latency_ms, created_at, response_json "
                        + "FROM kyc_biometric_attempt WHERE session_id=? ORDER BY created_at ASC",
                biometricSessionPk);
        List<Map<String, Object>> attemptDtos = new ArrayList<>();
        for (Map<String, Object> a : attempts) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("attemptId", a.get("public_id"));
            dto.put("step", a.get("step"));
            dto.put("attemptNo", a.get("attempt_no"));
            dto.put("outcome", a.get("outcome"));
            dto.put("livenessResult", a.get("liveness_result"));
            dto.put("confidenceScore", a.get("confidence_score"));
            dto.put("matchResult", a.get("match_result"));
            dto.put("similarity", a.get("similarity"));
            dto.put("failCode", a.get("fail_code"));
            dto.put("failMessage", a.get("fail_message"));
            dto.put("latencyMs", a.get("latency_ms"));
            dto.put("createdAt", a.get("created_at"));
            dto.put("responseJson", a.get("response_json"));
            attemptDtos.add(dto);
        }
        biometric.put("attempts", attemptDtos);
        row.put("biometric", biometric);
    }

    public void enrichKycListRow(Map<String, Object> row) {
        Long bioId = toLong(row.get("biometricSessionId"));
        if (bioId == null) {
            row.put("biometricStatus", null);
            row.put("biometricFailCode", null);
            row.put("faceSimilarity", null);
            return;
        }
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(
                "SELECT status, fail_reason_code, face_similarity FROM kyc_biometric_session WHERE id=?", bioId);
        if (sessions.isEmpty()) {
            row.put("biometricStatus", null);
            row.put("biometricFailCode", null);
            row.put("faceSimilarity", null);
            return;
        }
        Map<String, Object> s = sessions.get(0);
        row.put("biometricStatus", s.get("status"));
        row.put("biometricFailCode", s.get("fail_reason_code"));
        row.put("faceSimilarity", s.get("face_similarity"));
    }

    public String resolveObjectKey(Long kycId, String docType) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id_front_url, id_back_url, id_portrait_url, selfie_url, biometric_session_id FROM kyc_verification WHERE id=?",
                kycId);
        String key = switch (docType) {
            case "id-front" -> str(row.get("id_front_url"));
            case "id-back" -> str(row.get("id_back_url"));
            case "id-portrait" -> str(row.get("id_portrait_url"));
            case "selfie" -> str(row.get("selfie_url"));
            default -> null;
        };
        if (key != null) {
            return key;
        }
        Long bioId = toLong(row.get("biometric_session_id"));
        if (bioId == null) {
            return null;
        }
        Map<String, Object> s = jdbcTemplate.queryForMap("SELECT id_front_url, id_back_url, id_portrait_url, selfie_url FROM kyc_biometric_session WHERE id=?", bioId);
        return switch (docType) {
            case "id-front" -> str(s.get("id_front_url"));
            case "id-back" -> str(s.get("id_back_url"));
            case "id-portrait" -> str(s.get("id_portrait_url"));
            case "selfie" -> str(s.get("selfie_url"));
            default -> null;
        };
    }

    private static String docUrl(Long kycId, String type, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return "/api/admin/kyc/" + kycId + "/documents/" + type;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
