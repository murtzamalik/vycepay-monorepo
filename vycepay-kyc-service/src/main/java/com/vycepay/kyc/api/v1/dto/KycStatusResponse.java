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

    public KycStatusResponse() {
    }

    public KycStatusResponse(String status, String choiceOnboardingRequestId, String choiceAccountId) {
        this.status = status;
        this.displayStatus = KycDisplayStatus.fromRawStatus(status);
        this.choiceOnboardingRequestId = choiceOnboardingRequestId;
        this.choiceAccountId = choiceAccountId;
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
}
