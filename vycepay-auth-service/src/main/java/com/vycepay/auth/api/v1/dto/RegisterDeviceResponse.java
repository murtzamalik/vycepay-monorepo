package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response after registering an FCM device token.
 * Clients must persist {@code deviceId} to unregister on logout.
 */
@Schema(description = "Registered device identifiers for push notifications")
public class RegisterDeviceResponse {

    @Schema(description = "Persisted device_token row id used for DELETE /devices/{deviceId}", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deviceId;

    @Schema(description = "Device platform: ANDROID or IOS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String platform;

    public RegisterDeviceResponse() {
    }

    public RegisterDeviceResponse(Long deviceId, String platform) {
        this.deviceId = deviceId;
        this.platform = platform;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
