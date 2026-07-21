package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request for OTP verification.
 * Optional {@code fcmToken} registers the device for push (one token per customer).
 */
@Schema(description = "OTP verification request; optional FCM fields for push registration")
public class VerifyOtpRequest {

    @Schema(description = "Mobile country code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobileCountryCode;

    @Schema(description = "Mobile number", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @Schema(description = "OTP code received via SMS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpCode;

    @Schema(description = "FCM device token from Firebase SDK. Optional; omit if unavailable. "
            + "When present, replaces any existing push token for this customer (one device).")
    private String fcmToken;

    @Schema(description = "Device platform: ANDROID or IOS. Defaults to ANDROID when fcmToken is set.",
            example = "ANDROID")
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

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
