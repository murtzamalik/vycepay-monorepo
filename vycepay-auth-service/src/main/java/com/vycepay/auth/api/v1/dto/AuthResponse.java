package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Login / signup auth response. Token is null when device OTP or credential migration is required.
 */
@Schema(description = "Authentication response; token present only on full login success")
public class AuthResponse {

    @Schema(description = "JWT bearer token; null when deviceOtpRequired or mustSetCredentials")
    private String token;

    @Schema(description = "External customer ID")
    private String externalId;

    @Schema(description = "Token validity duration in seconds; 0 when no token")
    private long expiresIn;

    @Schema(description = "True when PIN ok but device not bound — client must verify device OTP then return to login")
    private boolean deviceOtpRequired;

    @Schema(description = "True when OTP for device bind or migrate was sent")
    private boolean otpSent;

    @Schema(description = "Masked mobile for OTP UX")
    private String maskedMobile;

    @Schema(description = "True when existing customer has no username/PIN — migrate flow required")
    private boolean mustSetCredentials;

    @Schema(description = "True after verify-device-otp successfully bound the device")
    private boolean deviceBound;

    @Schema(description = "Mobile country code when OTP flow is required")
    private String mobileCountryCode;

    @Schema(description = "Mobile number when OTP flow is required (for client verify calls)")
    private String mobile;

    public AuthResponse() {
    }

    public AuthResponse(String token, String externalId, long expiresIn) {
        this.token = token;
        this.externalId = externalId;
        this.expiresIn = expiresIn;
    }

    public static AuthResponse token(String token, String externalId, long expiresIn) {
        return new AuthResponse(token, externalId, expiresIn);
    }

    public static AuthResponse deviceOtpRequired(String externalId, String mobileCountryCode,
                                                 String mobile, String maskedMobile) {
        AuthResponse r = new AuthResponse();
        r.externalId = externalId;
        r.deviceOtpRequired = true;
        r.otpSent = true;
        r.mobileCountryCode = mobileCountryCode;
        r.mobile = mobile;
        r.maskedMobile = maskedMobile;
        return r;
    }

    public static AuthResponse mustSetCredentials(String externalId, String mobileCountryCode,
                                                  String mobile, String maskedMobile) {
        AuthResponse r = new AuthResponse();
        r.externalId = externalId;
        r.mustSetCredentials = true;
        r.otpSent = true;
        r.mobileCountryCode = mobileCountryCode;
        r.mobile = mobile;
        r.maskedMobile = maskedMobile;
        return r;
    }

    public static AuthResponse deviceBound() {
        AuthResponse r = new AuthResponse();
        r.deviceBound = true;
        return r;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public boolean isDeviceOtpRequired() {
        return deviceOtpRequired;
    }

    public void setDeviceOtpRequired(boolean deviceOtpRequired) {
        this.deviceOtpRequired = deviceOtpRequired;
    }

    public boolean isOtpSent() {
        return otpSent;
    }

    public void setOtpSent(boolean otpSent) {
        this.otpSent = otpSent;
    }

    public String getMaskedMobile() {
        return maskedMobile;
    }

    public void setMaskedMobile(String maskedMobile) {
        this.maskedMobile = maskedMobile;
    }

    public boolean isMustSetCredentials() {
        return mustSetCredentials;
    }

    public void setMustSetCredentials(boolean mustSetCredentials) {
        this.mustSetCredentials = mustSetCredentials;
    }

    public boolean isDeviceBound() {
        return deviceBound;
    }

    public void setDeviceBound(boolean deviceBound) {
        this.deviceBound = deviceBound;
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
}
