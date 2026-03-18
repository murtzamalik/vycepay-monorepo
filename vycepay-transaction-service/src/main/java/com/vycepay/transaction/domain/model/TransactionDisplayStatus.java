package com.vycepay.transaction.domain.model;

/**
 * Human-readable transaction status mapped from Choice Bank raw status codes.
 * Raw codes (from callback 0002 txStatus): 1=PENDING, 2=PROCESSING, 4=FAILED, 8=SUCCESS.
 */
public enum TransactionDisplayStatus {
    PENDING,
    PROCESSING,
    FAILED,
    SUCCESS;

    public static TransactionDisplayStatus fromRawStatus(String rawStatus) {
        if (rawStatus == null) return PENDING;
        return switch (rawStatus) {
            case "1" -> PENDING;
            case "2" -> PROCESSING;
            case "4" -> FAILED;
            case "8" -> SUCCESS;
            default -> PENDING;
        };
    }
}
