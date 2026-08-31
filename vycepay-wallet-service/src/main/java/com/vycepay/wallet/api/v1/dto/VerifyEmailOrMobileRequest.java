package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Activate a newly added email or mobile for future OTPs.
 * Identity is loaded from KYC internally.
 */
public class VerifyEmailOrMobileRequest {

    @Schema(description = "email or mobile (lowercase)", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "email")
    private String verifyType;

    public String getVerifyType() {
        return verifyType;
    }

    public void setVerifyType(String verifyType) {
        this.verifyType = verifyType;
    }
}
