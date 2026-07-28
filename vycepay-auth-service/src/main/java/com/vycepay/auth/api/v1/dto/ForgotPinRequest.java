package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Forgot-PIN request — sends PIN_RESET OTP to mobile.
 */
@Schema(description = "Request PIN reset OTP")
public class ForgotPinRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileCountryCode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

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
}
