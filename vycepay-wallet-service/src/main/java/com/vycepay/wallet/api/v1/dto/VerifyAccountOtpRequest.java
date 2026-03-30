package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Choice account/verifyOtp — distinct from common/confirmOperation (KYC/transaction OTP).
 */
public class VerifyAccountOtpRequest {

    @Schema(description = "Application id from a prior Choice account flow (e.g. email verify)")
    private String applicationId;

    @Schema(description = "OTP code")
    private String otpCode;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
