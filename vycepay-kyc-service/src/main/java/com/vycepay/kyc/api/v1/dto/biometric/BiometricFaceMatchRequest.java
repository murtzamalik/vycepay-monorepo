package com.vycepay.kyc.api.v1.dto.biometric;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Face match request (portrait crop + selfie, pass-through Base64)")
public class BiometricFaceMatchRequest {
    private String idPortraitBase64;
    private String selfieBase64;

    public String getIdPortraitBase64() { return idPortraitBase64; }
    public void setIdPortraitBase64(String idPortraitBase64) { this.idPortraitBase64 = idPortraitBase64; }
    public String getSelfieBase64() { return selfieBase64; }
    public void setSelfieBase64(String selfieBase64) { this.selfieBase64 = selfieBase64; }
}
