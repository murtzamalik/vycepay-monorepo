package com.vycepay.kyc.domain.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * KYC verification record. Maps to kyc_verification table.
 * Updated by callback 0001 (Personal Onboarding Result).
 */
@Entity
@Table(name = "kyc_verification")
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "choice_onboarding_request_id")
    private String choiceOnboardingRequestId;

    @Column(name = "choice_user_id")
    private String choiceUserId;

    @Column(name = "choice_account_id")
    private String choiceAccountId;

    @Column(name = "choice_account_type")
    private String choiceAccountType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "id_type")
    private String idType;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "id_front_url")
    private String idFrontUrl;

    @Column(name = "selfie_url")
    private String selfieUrl;

    @Column(name = "rejection_reason_ids", columnDefinition = "TEXT")
    private String rejectionReasonIds;

    @Column(name = "rejection_reason_msgs", columnDefinition = "TEXT")
    private String rejectionReasonMsgs;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getChoiceOnboardingRequestId() {
        return choiceOnboardingRequestId;
    }

    public void setChoiceOnboardingRequestId(String choiceOnboardingRequestId) {
        this.choiceOnboardingRequestId = choiceOnboardingRequestId;
    }

    public String getChoiceUserId() {
        return choiceUserId;
    }

    public void setChoiceUserId(String choiceUserId) {
        this.choiceUserId = choiceUserId;
    }

    public String getChoiceAccountId() {
        return choiceAccountId;
    }

    public void setChoiceAccountId(String choiceAccountId) {
        this.choiceAccountId = choiceAccountId;
    }

    public String getChoiceAccountType() {
        return choiceAccountType;
    }

    public void setChoiceAccountType(String choiceAccountType) {
        this.choiceAccountType = choiceAccountType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getIdFrontUrl() {
        return idFrontUrl;
    }

    public void setIdFrontUrl(String idFrontUrl) {
        this.idFrontUrl = idFrontUrl;
    }

    public String getSelfieUrl() {
        return selfieUrl;
    }

    public void setSelfieUrl(String selfieUrl) {
        this.selfieUrl = selfieUrl;
    }

    public String getRejectionReasonIds() {
        return rejectionReasonIds;
    }

    public void setRejectionReasonIds(String rejectionReasonIds) {
        this.rejectionReasonIds = rejectionReasonIds;
    }

    public String getRejectionReasonMsgs() {
        return rejectionReasonMsgs;
    }

    public void setRejectionReasonMsgs(String rejectionReasonMsgs) {
        this.rejectionReasonMsgs = rejectionReasonMsgs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
