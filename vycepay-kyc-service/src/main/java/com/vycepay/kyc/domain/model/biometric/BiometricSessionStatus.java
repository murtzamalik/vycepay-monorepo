package com.vycepay.kyc.domain.model.biometric;

/** Lifecycle status for a biometric KYC session. */
public enum BiometricSessionStatus {
    IN_PROGRESS,
    READY_TO_SUBMIT,
    SUBMITTED,
    FAILED,
    EXPIRED,
    CANCELLED
}
