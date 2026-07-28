package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Verify CREDENTIALS_MIGRATE OTP and receive short-lived JWT to call /credentials.
 */
@Schema(description = "Verify migrate OTP for existing users without PIN")
public class VerifyMigrateOtpRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileCountryCode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpCode;

    public String getMobileCountryCode() {
        return mobileCountryCode;
    }

    public void setMobileCountryCode(String mobileCountryCode) {
        this.mobileCountryCode = mobileCountryCode;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
