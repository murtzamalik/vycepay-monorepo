package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Confirm forgot-PIN with OTP and new PIN.
 */
@Schema(description = "Confirm PIN reset with OTP")
public class ForgotPinConfirmRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileCountryCode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpCode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPin;

    @Schema(description = "Optional IMEI to bind if customer has no device yet")
    private String imei;

    @Schema(description = "ANDROID or IOS")
    private String platform;

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

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
