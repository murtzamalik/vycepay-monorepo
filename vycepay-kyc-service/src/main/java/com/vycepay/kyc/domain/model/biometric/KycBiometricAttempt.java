package com.vycepay.kyc.domain.model.biometric;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Single WeVipers call attempt. Maps to kyc_biometric_attempt.
 */
@Entity
@Table(name = "kyc_biometric_attempt")
public class KycBiometricAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, length = 36)
    private String publicId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false, length = 32)
    private BiometricStep step;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private BiometricAttemptOutcome outcome;

    @Column(name = "vendor_http_status")
    private Integer vendorHttpStatus;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "liveness_result", length = 32)
    private String livenessResult;

    @Column(name = "confidence_score", precision = 6, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "match_result")
    private Boolean matchResult;

    @Column(name = "similarity", precision = 6, scale = 2)
    private BigDecimal similarity;

    @Column(name = "fail_code", length = 64)
    private String failCode;

    @Column(name = "fail_message", length = 512)
    private String failMessage;

    @Column(name = "media_url_1", length = 512)
    private String mediaUrl1;

    @Column(name = "media_url_2", length = 512)
    private String mediaUrl2;

    @Column(name = "request_meta_json", columnDefinition = "JSON")
    private String requestMetaJson;

    @Column(name = "response_json", columnDefinition = "JSON")
    private String responseJson;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public BiometricStep getStep() { return step; }
    public void setStep(BiometricStep step) { this.step = step; }
    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) { this.attemptNo = attemptNo; }
    public BiometricAttemptOutcome getOutcome() { return outcome; }
    public void setOutcome(BiometricAttemptOutcome outcome) { this.outcome = outcome; }
    public Integer getVendorHttpStatus() { return vendorHttpStatus; }
    public void setVendorHttpStatus(Integer vendorHttpStatus) { this.vendorHttpStatus = vendorHttpStatus; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public String getLivenessResult() { return livenessResult; }
    public void setLivenessResult(String livenessResult) { this.livenessResult = livenessResult; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public Boolean getMatchResult() { return matchResult; }
    public void setMatchResult(Boolean matchResult) { this.matchResult = matchResult; }
    public BigDecimal getSimilarity() { return similarity; }
    public void setSimilarity(BigDecimal similarity) { this.similarity = similarity; }
    public String getFailCode() { return failCode; }
    public void setFailCode(String failCode) { this.failCode = failCode; }
    public String getFailMessage() { return failMessage; }
    public void setFailMessage(String failMessage) { this.failMessage = failMessage; }
    public String getMediaUrl1() { return mediaUrl1; }
    public void setMediaUrl1(String mediaUrl1) { this.mediaUrl1 = mediaUrl1; }
    public String getMediaUrl2() { return mediaUrl2; }
    public void setMediaUrl2(String mediaUrl2) { this.mediaUrl2 = mediaUrl2; }
    public String getRequestMetaJson() { return requestMetaJson; }
    public void setRequestMetaJson(String requestMetaJson) { this.requestMetaJson = requestMetaJson; }
    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String responseJson) { this.responseJson = responseJson; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
