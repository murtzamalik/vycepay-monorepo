package com.vycepay.common.kyc.dto;

/**
 * Document or face liveness outcome from DocumentVerificationPort.
 */
public record LivenessResult(
        String livenessResult,
        Double confidenceScore,
        String kycRecommendation,
        String message,
        int httpStatus,
        String rawJson
) {
}
