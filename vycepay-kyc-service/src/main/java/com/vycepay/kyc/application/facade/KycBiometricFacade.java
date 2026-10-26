package com.vycepay.kyc.application.facade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.kyc.Base64Media;
import com.vycepay.common.kyc.dto.FaceMatchResult;
import com.vycepay.common.kyc.dto.LivenessResult;
import com.vycepay.common.kyc.dto.OcrResult;
import com.vycepay.common.kyc.port.DocumentVerificationPort;
import com.vycepay.common.logging.LoggingMdc;
import com.vycepay.common.logging.LoggingMdcKeys;
import com.vycepay.common.media.port.MediaStoragePort;
import com.vycepay.kyc.config.BiometricProperties;
import com.vycepay.kyc.domain.model.biometric.BiometricAttemptOutcome;
import com.vycepay.kyc.domain.model.biometric.BiometricSessionStatus;
import com.vycepay.kyc.domain.model.biometric.BiometricStep;
import com.vycepay.kyc.domain.model.biometric.KycBiometricAttempt;
import com.vycepay.kyc.domain.model.biometric.KycBiometricSession;
import com.vycepay.kyc.infrastructure.persistence.KycBiometricAttemptRepository;
import com.vycepay.kyc.infrastructure.persistence.KycBiometricSessionRepository;

/**
 * Orchestrates biometric KYC sessions: OCR, face liveness, face match (WeVipers),
 * media storage, and attempt audit trail. Document liveness is optional.
 */
@Service
@EnableConfigurationProperties(BiometricProperties.class)
public class KycBiometricFacade {

    private static final Logger log = LoggerFactory.getLogger(KycBiometricFacade.class);
    private static final List<BiometricStep> REQUIRED_STEPS = List.of(
            BiometricStep.OCR, BiometricStep.FACE_LIVENESS, BiometricStep.FACE_MATCH);

    private final KycBiometricSessionRepository sessionRepository;
    private final KycBiometricAttemptRepository attemptRepository;
    private final DocumentVerificationPort documentVerificationPort;
    private final MediaStoragePort mediaStoragePort;
    private final BiometricProperties properties;
    private final ObjectMapper objectMapper;

    public KycBiometricFacade(KycBiometricSessionRepository sessionRepository,
                              KycBiometricAttemptRepository attemptRepository,
                              DocumentVerificationPort documentVerificationPort,
                              MediaStoragePort mediaStoragePort,
                              BiometricProperties properties,
                              ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
        this.documentVerificationPort = documentVerificationPort;
        this.mediaStoragePort = mediaStoragePort;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isBiometricRequired() {
        return properties.isRequired();
    }

    /**
     * Creates or returns active biometric session for customer.
     */
    @Transactional
    public Map<String, Object> createSession(Long customerId, String idType, String documentCountry, boolean forceNew) {
        if (forceNew) {
            cancelActive(customerId);
        } else {
            var existing = sessionRepository.findFirstByCustomerIdAndStatusInOrderByCreatedAtDesc(
                    customerId, List.of(BiometricSessionStatus.IN_PROGRESS, BiometricSessionStatus.READY_TO_SUBMIT));
            if (existing.isPresent()) {
                KycBiometricSession s = markExpiredIfNeeded(existing.get());
                if (s.getStatus() == BiometricSessionStatus.IN_PROGRESS
                        || s.getStatus() == BiometricSessionStatus.READY_TO_SUBMIT) {
                    return sessionSummary(s);
                }
            }
        }
        KycBiometricSession session = new KycBiometricSession();
        session.setPublicId(UUID.randomUUID().toString());
        session.setCustomerId(customerId);
        session.setIdType(idType);
        session.setDocumentCountry(documentCountry != null ? documentCountry : "KE");
        session.setStatus(BiometricSessionStatus.IN_PROGRESS);
        session.setAttemptCount(0);
        session.setMaxAttempts(properties.getMaxAttempts());
        session.setExpiresAt(Instant.now().plus(properties.getSessionTtlHours(), ChronoUnit.HOURS));
        sessionRepository.save(session);
        LoggingMdc.put(LoggingMdcKeys.BIOMETRIC_SESSION_ID, session.getPublicId());
        log.info("Biometric session created customerId={} sessionId={}", customerId, session.getPublicId());
        return sessionSummary(session);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSession(Long customerId, String sessionPublicId) {
        KycBiometricSession session = loadOwned(customerId, sessionPublicId);
        session = ensureNotExpired(session);
        Map<String, Object> summary = sessionSummary(session);
        summary.put("steps", buildStepStatuses(session));
        summary.put("ocrPreview", ocrPreview(session));
        return summary;
    }

    @Transactional(readOnly = true)
    public KycBiometricSession findActiveForCustomer(Long customerId) {
        return sessionRepository.findFirstByCustomerIdAndStatusInOrderByCreatedAtDesc(
                        customerId, List.of(BiometricSessionStatus.IN_PROGRESS, BiometricSessionStatus.READY_TO_SUBMIT,
                                BiometricSessionStatus.SUBMITTED))
                .map(this::markExpiredIfNeeded)
                .filter(s -> s.getStatus() == BiometricSessionStatus.IN_PROGRESS
                        || s.getStatus() == BiometricSessionStatus.READY_TO_SUBMIT
                        || s.getStatus() == BiometricSessionStatus.SUBMITTED)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public KycBiometricSession requireReadyForSubmit(Long customerId, String sessionPublicId) {
        if (sessionPublicId == null || sessionPublicId.isBlank()) {
            throw new BusinessException("KYC_BIOMETRIC_REQUIRED",
                    "Biometric session is required", HttpStatus.CONFLICT);
        }
        KycBiometricSession session = loadOwned(customerId, sessionPublicId);
        session = ensureNotExpired(session);
        if (session.getStatus() != BiometricSessionStatus.READY_TO_SUBMIT) {
            throw new BusinessException("KYC_BIOMETRIC_NOT_READY",
                    "Biometric verification is not complete", HttpStatus.CONFLICT);
        }
        return session;
    }

    @Transactional
    public void markSubmitted(KycBiometricSession session, Long kycVerificationId) {
        session.setStatus(BiometricSessionStatus.SUBMITTED);
        session.setKycVerificationId(kycVerificationId);
        sessionRepository.save(session);
    }

    /**
     * Loads media bytes from session object key; returns empty if key blank.
     */
    public String loadMediaAsBase64(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return "";
        }
        byte[] bytes = mediaStoragePort.load(objectKey);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Transactional
    public Map<String, Object> documentLiveness(Long customerId, String sessionPublicId, String videoBase64) {
        KycBiometricSession session = prepareStep(customerId, sessionPublicId, BiometricStep.DOCUMENT_LIVENESS);
        byte[] bytes = Base64Media.decodeRequired(videoBase64, "videoBase64");
        String mediaKey = store(session, bytes, "video/mp4", "doc-liveness.mp4");
        session.setDocVideoUrl(mediaKey);

        long start = System.currentTimeMillis();
        LivenessResult result = documentVerificationPort.documentLiveness(videoBase64);
        int latency = (int) (System.currentTimeMillis() - start);

        BiometricAttemptOutcome outcome;
        String failCode = null;
        boolean retryable = false;
        String lr = result.livenessResult() != null ? result.livenessResult().toUpperCase() : "";
        if (result.httpStatus() >= 500) {
            outcome = BiometricAttemptOutcome.RETRYABLE;
            failCode = "VENDOR_5XX";
            retryable = true;
        } else if ("LIVE".equals(lr)) {
            outcome = BiometricAttemptOutcome.PASS;
        } else if ("FAKE".equals(lr)) {
            outcome = BiometricAttemptOutcome.FAIL;
            failCode = "DOCUMENT_FAKE";
        } else {
            outcome = BiometricAttemptOutcome.RETRYABLE;
            failCode = "DOCUMENT_INCONCLUSIVE";
            retryable = true;
        }

        KycBiometricAttempt attempt = saveAttempt(session, BiometricStep.DOCUMENT_LIVENESS, outcome,
                result.httpStatus(), latency, lr, result.confidenceScore(), null, null,
                failCode, result.message(), mediaKey, null, result.rawJson());

        if (outcome == BiometricAttemptOutcome.FAIL) {
            session.setStatus(BiometricSessionStatus.FAILED);
            session.setFailReasonCode(failCode);
            session.setFailReasonMsg(result.message());
        }
        session.setCurrentStep(BiometricStep.DOCUMENT_LIVENESS);
        sessionRepository.save(session);

        Map<String, Object> extra = new HashMap<>();
        extra.put("livenessResult", lr);
        extra.put("confidenceScore", result.confidenceScore());
        return stepResponse(attempt, session, outcome, failCode, retryable, result.message(), extra);
    }

    @Transactional
    public Map<String, Object> ocr(Long customerId, String sessionPublicId, String documentKind,
                                   String frontImageBase64, String backImageBase64) {
        KycBiometricSession session = prepareStep(customerId, sessionPublicId, BiometricStep.OCR);
        byte[] front = Base64Media.decodeRequired(frontImageBase64, "frontImageBase64");
        String frontKey = store(session, front, "image/jpeg", "id-front.jpg");
        session.setIdFrontUrl(frontKey);
        String backKey = null;
        if (backImageBase64 != null && !backImageBase64.isBlank()) {
            byte[] back = Base64Media.decodeRequired(backImageBase64, "backImageBase64");
            backKey = store(session, back, "image/jpeg", "id-back.jpg");
            session.setIdBackUrl(backKey);
        }

        long start = System.currentTimeMillis();
        OcrResult result = documentVerificationPort.extract(documentKind, frontImageBase64, backImageBase64);
        int latency = (int) (System.currentTimeMillis() - start);

        BiometricAttemptOutcome outcome;
        String failCode = null;
        boolean retryable = false;
        boolean hasFields = result.extracted() != null && !result.extracted().isEmpty();
        if (result.httpStatus() >= 500) {
            outcome = BiometricAttemptOutcome.RETRYABLE;
            failCode = "VENDOR_5XX";
            retryable = true;
        } else if (result.httpStatus() >= 400) {
            outcome = BiometricAttemptOutcome.RETRYABLE;
            failCode = "KYC_BIOMETRIC_BAD_INPUT";
            retryable = true;
        } else if (hasFields) {
            outcome = BiometricAttemptOutcome.PASS;
            session.setOcrPayloadJson(toJson(result.rawPayload() != null ? result.rawPayload() : result.extracted()));
        } else {
            outcome = BiometricAttemptOutcome.RETRYABLE;
            failCode = "OCR_EMPTY";
            retryable = true;
        }

        KycBiometricAttempt attempt = saveAttempt(session, BiometricStep.OCR, outcome,
                result.httpStatus(), latency, null, null, null, null,
                failCode, result.message(), frontKey, backKey, result.rawJson());
        session.setCurrentStep(BiometricStep.OCR);
        sessionRepository.save(session);

        Map<String, Object> extra = new HashMap<>();
        extra.put("detectedSide", result.detectedSide());
        extra.put("extracted", result.extracted());
        return stepResponse(attempt, session, outcome, failCode, retryable, result.message(), extra);
    }

    @Transactional
    public Map<String, Object> faceLiveness(Long customerId, String sessionPublicId, String videoBase64) {
        KycBiometricSession session = prepareStep(customerId, sessionPublicId, BiometricStep.FACE_LIVENESS);
        requirePriorPass(session, BiometricStep.OCR);
        byte[] bytes = Base64Media.decodeRequired(videoBase64, "videoBase64");
        String mediaKey = store(session, bytes, "video/mp4", "face-liveness.mp4");
        session.setFaceVideoUrl(mediaKey);

        long start = System.currentTimeMillis();
        LivenessResult result = documentVerificationPort.faceLiveness(videoBase64);
        int latency = (int) (System.currentTimeMillis() - start);

        BiometricAttemptOutcome outcome;
        String failCode = null;
        boolean retryable = false;
        String lr = result.livenessResult() != null ? result.livenessResult().toUpperCase() : "";
        if (result.httpStatus() >= 500) {
            outcome = BiometricAttemptOutcome.RETRYABLE;
            failCode = "VENDOR_5XX";
            retryable = true;
        } else if ("LIVE".equals(lr)) {
            String rec = result.kycRecommendation();
            if (rec != null && !rec.isBlank() && !"APPROVE".equalsIgnoreCase(rec)) {
                outcome = BiometricAttemptOutcome.FAIL;
                failCode = "FACE_SPOOF";
            } else {
                outcome = BiometricAttemptOutcome.PASS;
            }
        } else if ("SPOOF".equals(lr)) {
            outcome = BiometricAttemptOutcome.FAIL;
            failCode = "FACE_SPOOF";
        } else {
            outcome = BiometricAttemptOutcome.RETRYABLE;
            failCode = "FACE_INCONCLUSIVE";
            retryable = true;
        }

        KycBiometricAttempt attempt = saveAttempt(session, BiometricStep.FACE_LIVENESS, outcome,
                result.httpStatus(), latency, lr, result.confidenceScore(), null, null,
                failCode, result.message(), mediaKey, null, result.rawJson());

        if (outcome == BiometricAttemptOutcome.FAIL) {
            session.setStatus(BiometricSessionStatus.FAILED);
            session.setFailReasonCode(failCode);
            session.setFailReasonMsg(result.message());
        }
        session.setCurrentStep(BiometricStep.FACE_LIVENESS);
        sessionRepository.save(session);

        Map<String, Object> extra = new HashMap<>();
        extra.put("livenessResult", lr);
        extra.put("confidenceScore", result.confidenceScore());
        extra.put("kycRecommendation", result.kycRecommendation() != null ? result.kycRecommendation() : "");
        return stepResponse(attempt, session, outcome, failCode, retryable, result.message(), extra);
    }

    @Transactional
    public Map<String, Object> faceMatch(Long customerId, String sessionPublicId,
                                         String idPortraitBase64, String selfieBase64) {
        KycBiometricSession session = prepareStep(customerId, sessionPublicId, BiometricStep.FACE_MATCH);
        requirePriorPass(session, BiometricStep.OCR);
        requirePriorPass(session, BiometricStep.FACE_LIVENESS);

        byte[] portrait = Base64Media.decodeRequired(idPortraitBase64, "idPortraitBase64");
        byte[] selfie = Base64Media.decodeRequired(selfieBase64, "selfieBase64");
        String portraitKey = store(session, portrait, "image/jpeg", "id-portrait.jpg");
        String selfieKey = store(session, selfie, "image/jpeg", "selfie.jpg");
        session.setIdPortraitUrl(portraitKey);
        session.setSelfieUrl(selfieKey);

        long start = System.currentTimeMillis();
        FaceMatchResult result = documentVerificationPort.match(idPortraitBase64, selfieBase64);
        int latency = (int) (System.currentTimeMillis() - start);

        BiometricAttemptOutcome outcome;
        String failCode = null;
        boolean retryable = false;
        if (result.httpStatus() >= 500) {
            String msg = result.message() != null ? result.message() : "";
            if (msg.toLowerCase().contains("invalidparameter") || msg.toLowerCase().contains("crop")) {
                outcome = BiometricAttemptOutcome.RETRYABLE;
                failCode = "BAD_CROP";
            } else {
                outcome = BiometricAttemptOutcome.RETRYABLE;
                failCode = "VENDOR_5XX";
            }
            retryable = true;
        } else if (result.match()) {
            outcome = BiometricAttemptOutcome.PASS;
            session.setFaceMatch(true);
            if (result.similarity() != null) {
                session.setFaceSimilarity(BigDecimal.valueOf(result.similarity()));
            }
            session.setStatus(BiometricSessionStatus.READY_TO_SUBMIT);
            session.setFailReasonCode(null);
            session.setFailReasonMsg(null);
        } else {
            outcome = BiometricAttemptOutcome.FAIL;
            failCode = "FACE_MISMATCH";
            session.setFaceMatch(false);
            if (result.similarity() != null) {
                session.setFaceSimilarity(BigDecimal.valueOf(result.similarity()));
            }
            int stepAttempts = attemptRepository.countBySessionIdAndStep(session.getId(), BiometricStep.FACE_MATCH) + 1;
            if (stepAttempts >= session.getMaxAttempts()) {
                session.setStatus(BiometricSessionStatus.FAILED);
                session.setFailReasonCode(failCode);
                session.setFailReasonMsg(result.message());
            } else {
                retryable = true;
                outcome = BiometricAttemptOutcome.RETRYABLE;
            }
        }

        KycBiometricAttempt attempt = saveAttempt(session, BiometricStep.FACE_MATCH, outcome,
                result.httpStatus(), latency, null, null, result.match(), result.similarity(),
                failCode, result.message(), portraitKey, selfieKey, result.rawJson());
        session.setCurrentStep(BiometricStep.FACE_MATCH);
        sessionRepository.save(session);

        Map<String, Object> extra = new HashMap<>();
        extra.put("match", result.match());
        extra.put("similarity", result.similarity());
        return stepResponse(attempt, session, outcome, failCode, retryable, result.message(), extra);
    }

    private void requirePriorPass(KycBiometricSession session, BiometricStep step) {
        boolean passed = attemptRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .anyMatch(a -> a.getStep() == step && a.getOutcome() == BiometricAttemptOutcome.PASS);
        if (!passed) {
            throw new BusinessException("KYC_BIOMETRIC_NOT_READY",
                    "Complete " + step.name() + " before this step", HttpStatus.CONFLICT);
        }
    }

    private KycBiometricSession prepareStep(Long customerId, String sessionPublicId, BiometricStep step) {
        KycBiometricSession session = loadOwned(customerId, sessionPublicId);
        session = ensureNotExpired(session);
        if (session.getStatus() == BiometricSessionStatus.FAILED
                || session.getStatus() == BiometricSessionStatus.CANCELLED
                || session.getStatus() == BiometricSessionStatus.SUBMITTED) {
            throw new BusinessException("KYC_BIOMETRIC_NOT_READY",
                    "Session is not active", HttpStatus.CONFLICT);
        }
        LoggingMdc.put(LoggingMdcKeys.BIOMETRIC_SESSION_ID, session.getPublicId());
        LoggingMdc.put(LoggingMdcKeys.LABELS_FLOW, "kyc_biometric_" + step.name().toLowerCase());
        return session;
    }

    private KycBiometricSession loadOwned(Long customerId, String sessionPublicId) {
        KycBiometricSession session = sessionRepository.findByPublicId(sessionPublicId)
                .orElseThrow(() -> new BusinessException("KYC_BIOMETRIC_SESSION_NOT_FOUND",
                        "Session not found", HttpStatus.NOT_FOUND));
        if (!session.getCustomerId().equals(customerId)) {
            throw new BusinessException("FORBIDDEN", "Session does not belong to this customer", HttpStatus.FORBIDDEN);
        }
        return session;
    }

    /**
     * Marks session EXPIRED when past TTL; does not throw (for status / create flows).
     */
    private KycBiometricSession markExpiredIfNeeded(KycBiometricSession session) {
        if (session.getExpiresAt() != null
                && session.getExpiresAt().isBefore(Instant.now())
                && (session.getStatus() == BiometricSessionStatus.IN_PROGRESS
                || session.getStatus() == BiometricSessionStatus.READY_TO_SUBMIT)) {
            session.setStatus(BiometricSessionStatus.EXPIRED);
            sessionRepository.save(session);
        }
        return session;
    }

    private KycBiometricSession ensureNotExpired(KycBiometricSession session) {
        session = markExpiredIfNeeded(session);
        if (session.getStatus() == BiometricSessionStatus.EXPIRED) {
            throw new BusinessException("KYC_BIOMETRIC_SESSION_EXPIRED",
                    "Session expired", HttpStatus.GONE);
        }
        return session;
    }

    private void cancelActive(Long customerId) {
        sessionRepository.findFirstByCustomerIdAndStatusInOrderByCreatedAtDesc(
                        customerId, List.of(BiometricSessionStatus.IN_PROGRESS, BiometricSessionStatus.READY_TO_SUBMIT))
                .ifPresent(s -> {
                    s.setStatus(BiometricSessionStatus.CANCELLED);
                    sessionRepository.save(s);
                });
    }

    private String store(KycBiometricSession session, byte[] bytes, String contentType, String hint) {
        String prefix = "kyc/" + session.getCustomerId() + "/" + session.getPublicId();
        return mediaStoragePort.store(prefix, contentType, bytes, hint);
    }

    private KycBiometricAttempt saveAttempt(KycBiometricSession session, BiometricStep step,
                                            BiometricAttemptOutcome outcome, int httpStatus, int latencyMs,
                                            String livenessResult, Double confidence, Boolean match, Double similarity,
                                            String failCode, String failMessage, String media1, String media2,
                                            String responseJson) {
        int attemptNo = attemptRepository.countBySessionIdAndStep(session.getId(), step) + 1;
        KycBiometricAttempt attempt = new KycBiometricAttempt();
        attempt.setPublicId(UUID.randomUUID().toString());
        attempt.setSessionId(session.getId());
        attempt.setCustomerId(session.getCustomerId());
        attempt.setStep(step);
        attempt.setAttemptNo(attemptNo);
        attempt.setOutcome(outcome);
        attempt.setVendorHttpStatus(httpStatus);
        attempt.setLatencyMs(latencyMs);
        attempt.setLivenessResult(livenessResult);
        if (confidence != null) attempt.setConfidenceScore(BigDecimal.valueOf(confidence));
        attempt.setMatchResult(match);
        if (similarity != null) attempt.setSimilarity(BigDecimal.valueOf(similarity));
        attempt.setFailCode(failCode);
        attempt.setFailMessage(truncate(failMessage, 512));
        attempt.setMediaUrl1(media1);
        attempt.setMediaUrl2(media2);
        attempt.setResponseJson(sanitizeJson(responseJson));
        attemptRepository.save(attempt);
        session.setAttemptCount(session.getAttemptCount() + 1);
        LoggingMdc.put(LoggingMdcKeys.BIOMETRIC_ATTEMPT_ID, attempt.getPublicId());
        log.info("Biometric attempt sessionId={} step={} outcome={} failCode={} latencyMs={}",
                session.getPublicId(), step, outcome, failCode, latencyMs);
        return attempt;
    }

    private Map<String, Object> stepResponse(KycBiometricAttempt attempt, KycBiometricSession session,
                                             BiometricAttemptOutcome outcome, String failCode, boolean retryable,
                                             String message, Map<String, Object> extra) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("attemptId", attempt.getPublicId());
        resp.put("outcome", outcome.name());
        resp.put("failCode", failCode);
        resp.put("retryable", retryable);
        resp.put("nextStep", nextStep(session, outcome));
        resp.put("sessionStatus", session.getStatus().name());
        resp.put("canSubmit", session.getStatus() == BiometricSessionStatus.READY_TO_SUBMIT);
        resp.put("message", message != null ? message : "");
        if (extra != null) {
            resp.putAll(extra);
        }
        return resp;
    }

    private String nextStep(KycBiometricSession session, BiometricAttemptOutcome outcome) {
        if (session.getStatus() == BiometricSessionStatus.READY_TO_SUBMIT
                || session.getStatus() == BiometricSessionStatus.FAILED) {
            return null;
        }
        if (outcome != BiometricAttemptOutcome.PASS) {
            return session.getCurrentStep() != null ? session.getCurrentStep().name() : BiometricStep.OCR.name();
        }
        Set<BiometricStep> passed = EnumSet.noneOf(BiometricStep.class);
        for (KycBiometricAttempt a : attemptRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())) {
            if (a.getOutcome() == BiometricAttemptOutcome.PASS) {
                passed.add(a.getStep());
            }
        }
        for (BiometricStep required : REQUIRED_STEPS) {
            if (!passed.contains(required)) {
                return required.name();
            }
        }
        return null;
    }

    private Map<String, Object> sessionSummary(KycBiometricSession session) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sessionId", session.getPublicId());
        m.put("status", session.getStatus().name());
        m.put("requiredSteps", REQUIRED_STEPS.stream().map(Enum::name).toList());
        m.put("expiresAt", session.getExpiresAt() != null ? session.getExpiresAt().toString() : null);
        m.put("currentStep", session.getCurrentStep() != null ? session.getCurrentStep().name() : null);
        m.put("failReasonCode", session.getFailReasonCode());
        m.put("failReasonMsg", session.getFailReasonMsg());
        m.put("canSubmit", session.getStatus() == BiometricSessionStatus.READY_TO_SUBMIT);
        m.put("faceMatch", session.getFaceMatch());
        m.put("faceSimilarity", session.getFaceSimilarity());
        return m;
    }

    private List<Map<String, Object>> buildStepStatuses(KycBiometricSession session) {
        List<KycBiometricAttempt> attempts = attemptRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<Map<String, Object>> steps = new ArrayList<>();
        for (BiometricStep step : List.of(BiometricStep.DOCUMENT_LIVENESS, BiometricStep.OCR,
                BiometricStep.FACE_LIVENESS, BiometricStep.FACE_MATCH)) {
            List<KycBiometricAttempt> stepAttempts = attempts.stream().filter(a -> a.getStep() == step).toList();
            if (step == BiometricStep.DOCUMENT_LIVENESS && stepAttempts.isEmpty()) {
                continue;
            }
            String status = "PENDING";
            if (!stepAttempts.isEmpty()) {
                KycBiometricAttempt last = stepAttempts.get(stepAttempts.size() - 1);
                status = last.getOutcome().name();
                if (stepAttempts.stream().anyMatch(a -> a.getOutcome() == BiometricAttemptOutcome.PASS)) {
                    status = "PASS";
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("step", step.name());
            row.put("status", status);
            row.put("attempts", stepAttempts.size());
            steps.add(row);
        }
        return steps;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ocrPreview(KycBiometricSession session) {
        if (session.getOcrPayloadJson() == null || session.getOcrPayloadJson().isBlank()) {
            return null;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(session.getOcrPayloadJson(), Map.class);
            Map<String, Object> preview = new LinkedHashMap<>();
            Object front = raw.get("frontSide");
            if (front instanceof Map<?, ?> fm && fm.get("english") instanceof Map<?, ?> eng) {
                preview.put("idNumber", eng.get("id_number") != null ? eng.get("id_number") : eng.get("idNumber"));
                preview.put("fullName", eng.get("name") != null ? eng.get("name") : eng.get("full_name"));
                preview.put("birthday", eng.get("date_of_birth") != null ? eng.get("date_of_birth") : eng.get("dob"));
                preview.put("gender", eng.get("gender"));
            } else {
                preview.put("idNumber", raw.get("id_number") != null ? raw.get("id_number") : raw.get("idNumber"));
                preview.put("fullName", raw.get("name") != null ? raw.get("name") : raw.get("fullName"));
                preview.put("birthday", raw.get("birthday") != null ? raw.get("birthday") : raw.get("date_of_birth"));
                preview.put("gender", raw.get("gender"));
            }
            return preview;
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return null;
        String trimmed = raw.length() > 100_000 ? raw.substring(0, 100_000) : raw;
        // Drop accidental base64 blobs from stored vendor JSON
        return trimmed.replaceAll("\"[A-Za-z0-9+/]{200,}={0,2}\"", "\"[omitted]\"");
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
