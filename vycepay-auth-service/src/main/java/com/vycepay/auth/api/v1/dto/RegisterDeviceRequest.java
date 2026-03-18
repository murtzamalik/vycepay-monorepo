package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to register a device FCM token for push notifications.
 */
@Schema(description = "Device registration for push notifications")
public class RegisterDeviceRequest {

    @Schema(description = "FCM device token from Firebase SDK", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fcmToken;

    @Schema(description = "Device platform: ANDROID or IOS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String platform;

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
