package com.vycepay.kyc.domain.model;

/**
 * Human-readable KYC status mapped from raw Choice Bank onboarding status codes.
 * Raw codes (from callback 0001): 1=submitted/pending, 7=account opened/approved.
 */
public enum KycDisplayStatus {
    NOT_STARTED,
    PENDING,
    APPROVED,
    REJECTED;

    public static KycDisplayStatus fromRawStatus(String rawStatus) {
        if (rawStatus == null || "NOT_STARTED".equals(rawStatus)) return NOT_STARTED;
        return switch (rawStatus) {
            case "1" -> PENDING;
            case "7" -> APPROVED;
            default -> REJECTED;
        };
    }
}
