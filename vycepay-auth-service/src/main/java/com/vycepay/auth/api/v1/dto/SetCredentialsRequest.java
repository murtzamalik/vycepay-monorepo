package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Set username + PIN once during onboarding or migration.
 */
@Schema(description = "Set username and 4-digit PIN (once)")
public class SetCredentialsRequest {

    @Schema(description = "Username 3–20 chars", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "4-digit PIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pin;

    @Schema(description = "Optional IMEI to bind if not already bound")
    private String imei;

    @Schema(description = "ANDROID or IOS")
    private String platform;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
