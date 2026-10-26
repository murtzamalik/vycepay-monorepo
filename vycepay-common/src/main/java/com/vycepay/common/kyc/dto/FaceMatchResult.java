package com.vycepay.common.kyc.dto;

/**
 * Face match outcome from DocumentVerificationPort.
 */
public record FaceMatchResult(
        boolean match,
        Double similarity,
        String message,
        int httpStatus,
        String rawJson
) {
}
