package com.vycepay.common.kyc.dto;

import java.util.Map;

/**
 * OCR extraction outcome from DocumentVerificationPort.
 */
public record OcrResult(
        String detectedSide,
        Map<String, Object> extracted,
        Map<String, Object> rawPayload,
        String message,
        int httpStatus,
        String rawJson
) {
}
