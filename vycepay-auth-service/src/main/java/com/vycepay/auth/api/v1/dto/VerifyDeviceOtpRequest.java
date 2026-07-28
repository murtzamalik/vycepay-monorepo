package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Device-bind OTP verification. Does not issue JWT — client returns to login.
 */
@Schema(description = "Verify DEVICE_BIND OTP and bind IMEI")
public class VerifyDeviceOtpRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileCountryCode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpCode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
