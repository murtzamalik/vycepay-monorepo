package com.vycepay.kyc.api.v1.dto.biometric;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Base64 media for liveness")
public class BiometricMediaRequest {
    private String videoBase64;
    public String getVideoBase64() { return videoBase64; }
    public void setVideoBase64(String videoBase64) { this.videoBase64 = videoBase64; }
}
