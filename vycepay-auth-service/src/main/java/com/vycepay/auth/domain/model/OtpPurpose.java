package com.vycepay.auth.domain.model;

/**
 * Distinguishes auth OTP use-cases so codes cannot be reused across flows.
 */
public enum OtpPurpose {
    SIGNUP,
    DEVICE_BIND,
    PIN_RESET,
    CREDENTIALS_MIGRATE
}
