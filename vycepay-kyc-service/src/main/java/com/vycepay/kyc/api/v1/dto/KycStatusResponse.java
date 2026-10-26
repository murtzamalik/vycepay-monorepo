package com.vycepay.kyc.api.v1.dto;

import com.vycepay.kyc.domain.model.KycDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * KYC status response.
 */
@Schema(description = "KYC verification status")
public class KycStatusResponse {

    @Schema(description = "Raw status code from Choice Bank (1=submitted/pending, 7=account opened/approved)")
    private String status;

    @Schema(description = "Human-readable status (NOT_STARTED, PENDING, APPROVED, REJECTED)")
    private KycDisplayStatus displayStatus;

    @Schema(description = "Choice Bank onboarding request ID")
    private String choiceOnboardingRequestId;

    @Schema(description = "Choice Bank account ID when approved")
    private String choiceAccountId;

    @Schema(description = "Customer-facing message from Choice Bank when present")
    private String message;

    @Schema(description = "Active biometric session id when present")
    private String biometricSessionId;

    @Schema(description = "Biometric session status")
    private String biometricStatus;

    @Schema(description = "True when biometric session is READY_TO_SUBMIT")
    private Boolean biometricCanSubmit;

    public KycStatusResponse() {
    }

    public KycStatusResponse(String status, String choiceOnboardingRequestId, String choiceAccountId) {
        this(status, choiceOnboardingRequestId, choiceAccountId, null);
    }

    public KycStatusResponse(String status, String choiceOnboardingRequestId, String choiceAccountId, String message) {
        this.status = status;
        this.displayStatus = KycDisplayStatus.fromRawStatus(status);
        this.choiceOnboardingRequestId = choiceOnboardingRequestId;
        this.choiceAccountId = choiceAccountId;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public KycDisplayStatus getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(KycDisplayStatus displayStatus) {
        this.displayStatus = displayStatus;
    }

    public String getChoiceOnboardingRequestId() {
        return choiceOnboardingRequestId;
    }

    public void setChoiceOnboardingRequestId(String choiceOnboardingRequestId) {
        this.choiceOnboardingRequestId = choiceOnboardingRequestId;
    }

    public String getChoiceAccountId() {
        return choiceAccountId;
    }

    public void setChoiceAccountId(String choiceAccountId) {
        this.choiceAccountId = choiceAccountId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBiometricSessionId() {
        return biometricSessionId;
    }

    public void setBiometricSessionId(String biometricSessionId) {
        this.biometricSessionId = biometricSessionId;
    }

    public String getBiometricStatus() {
        return biometricStatus;
    }

    public void setBiometricStatus(String biometricStatus) {
        this.biometricStatus = biometricStatus;
    }

    public Boolean getBiometricCanSubmit() {
        return biometricCanSubmit;
    }

    public void setBiometricCanSubmit(Boolean biometricCanSubmit) {
        this.biometricCanSubmit = biometricCanSubmit;
    }
}
