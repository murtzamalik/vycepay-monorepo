package com.vycepay.kyc.domain.model.biometric;

/** Biometric verification steps. DOCUMENT_LIVENESS is optional. */
public enum BiometricStep {
    DOCUMENT_LIVENESS,
    OCR,
    FACE_LIVENESS,
    FACE_MATCH,
    TRANSLATE
}
