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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Biometric pre-KYC session. Maps to kyc_biometric_session.
 */
@Entity
@Table(name = "kyc_biometric_session")
public class KycBiometricSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, length = 36)
    private String publicId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "id_type", length = 16)
    private String idType;

    @Column(name = "document_country", length = 8)
    private String documentCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BiometricSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", length = 32)
    private BiometricStep currentStep;

    @Column(name = "fail_reason_code", length = 64)
    private String failReasonCode;

    @Column(name = "fail_reason_msg", length = 512)
    private String failReasonMsg;

    @Column(name = "id_front_url", length = 512)
    private String idFrontUrl;

    @Column(name = "id_back_url", length = 512)
    private String idBackUrl;

    @Column(name = "id_portrait_url", length = 512)
    private String idPortraitUrl;

    @Column(name = "selfie_url", length = 512)
    private String selfieUrl;

    @Column(name = "doc_video_url", length = 512)
    private String docVideoUrl;

    @Column(name = "face_video_url", length = 512)
    private String faceVideoUrl;

    @Column(name = "ocr_payload_json", columnDefinition = "JSON")
    private String ocrPayloadJson;

    @Column(name = "face_similarity", precision = 6, scale = 2)
    private BigDecimal faceSimilarity;

    @Column(name = "face_match")
    private Boolean faceMatch;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "kyc_verification_id")
    private Long kycVerificationId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }
    public String getDocumentCountry() { return documentCountry; }
    public void setDocumentCountry(String documentCountry) { this.documentCountry = documentCountry; }
    public BiometricSessionStatus getStatus() { return status; }
    public void setStatus(BiometricSessionStatus status) { this.status = status; }
    public BiometricStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(BiometricStep currentStep) { this.currentStep = currentStep; }
    public String getFailReasonCode() { return failReasonCode; }
    public void setFailReasonCode(String failReasonCode) { this.failReasonCode = failReasonCode; }
    public String getFailReasonMsg() { return failReasonMsg; }
    public void setFailReasonMsg(String failReasonMsg) { this.failReasonMsg = failReasonMsg; }
    public String getIdFrontUrl() { return idFrontUrl; }
    public void setIdFrontUrl(String idFrontUrl) { this.idFrontUrl = idFrontUrl; }
    public String getIdBackUrl() { return idBackUrl; }
    public void setIdBackUrl(String idBackUrl) { this.idBackUrl = idBackUrl; }
    public String getIdPortraitUrl() { return idPortraitUrl; }
    public void setIdPortraitUrl(String idPortraitUrl) { this.idPortraitUrl = idPortraitUrl; }
    public String getSelfieUrl() { return selfieUrl; }
    public void setSelfieUrl(String selfieUrl) { this.selfieUrl = selfieUrl; }
    public String getDocVideoUrl() { return docVideoUrl; }
    public void setDocVideoUrl(String docVideoUrl) { this.docVideoUrl = docVideoUrl; }
    public String getFaceVideoUrl() { return faceVideoUrl; }
    public void setFaceVideoUrl(String faceVideoUrl) { this.faceVideoUrl = faceVideoUrl; }
    public String getOcrPayloadJson() { return ocrPayloadJson; }
    public void setOcrPayloadJson(String ocrPayloadJson) { this.ocrPayloadJson = ocrPayloadJson; }
    public BigDecimal getFaceSimilarity() { return faceSimilarity; }
    public void setFaceSimilarity(BigDecimal faceSimilarity) { this.faceSimilarity = faceSimilarity; }
    public Boolean getFaceMatch() { return faceMatch; }
    public void setFaceMatch(Boolean faceMatch) { this.faceMatch = faceMatch; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Long getKycVerificationId() { return kycVerificationId; }
    public void setKycVerificationId(Long kycVerificationId) { this.kycVerificationId = kycVerificationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
