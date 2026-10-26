package com.vycepay.kyc.api.v1.dto.biometric;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OCR request")
public class BiometricOcrRequest {
    private String documentKind;
    private String frontImageBase64;
    private String backImageBase64;

    public String getDocumentKind() { return documentKind; }
    public void setDocumentKind(String documentKind) { this.documentKind = documentKind; }
    public String getFrontImageBase64() { return frontImageBase64; }
    public void setFrontImageBase64(String frontImageBase64) { this.frontImageBase64 = frontImageBase64; }
    public String getBackImageBase64() { return backImageBase64; }
    public void setBackImageBase64(String backImageBase64) { this.backImageBase64 = backImageBase64; }
}
