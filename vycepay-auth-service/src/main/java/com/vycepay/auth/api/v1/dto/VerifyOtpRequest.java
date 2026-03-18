package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request for OTP verification.
 */
@Schema(description = "OTP verification request")
public class VerifyOtpRequest {

    @Schema(description = "Mobile country code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileCountryCode;

    @Schema(description = "Mobile number", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @Schema(description = "OTP code received via SMS", requiredMode = Schema.RequiredMode.REQUIRED)
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
