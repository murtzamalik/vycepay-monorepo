package com.vycepay.kyc.domain.model.biometric;

/** Outcome of a single WeVipers attempt. */
public enum BiometricAttemptOutcome {
    PASS,
    FAIL,
    RETRYABLE,
    ERROR
}
