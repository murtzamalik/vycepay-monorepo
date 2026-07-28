package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * PIN login request. Provide username OR mobile pair, plus PIN and IMEI.
 */
@Schema(description = "Login with username or mobile + PIN + device IMEI")
public class LoginRequest {

    @Schema(description = "Username (alternative to mobile)")
    private String username;

    @Schema(description = "Mobile country code when logging in with mobile")
    private String mobileCountryCode;

    @Schema(description = "Mobile number when logging in with mobile")
    private String mobile;

    @Schema(description = "4-digit PIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pin;

    @Schema(description = "Device fingerprint (ANDROID_ID / IMEI)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String imei;

    @Schema(description = "Optional FCM token; bound on successful JWT login")
    private String fcmToken;

    @Schema(description = "ANDROID or IOS")
    private String platform;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
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
