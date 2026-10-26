package com.vycepay.common.kyc.port;

import com.vycepay.common.kyc.dto.FaceMatchResult;
import com.vycepay.common.kyc.dto.LivenessResult;
import com.vycepay.common.kyc.dto.OcrResult;

/**
 * Port for biometric document verification (OCR, liveness, face match).
 * Implemented by WeVipers adapter; domain/facades depend on this port only.
 *
 * Callers: KycBiometricFacade (step orchestration). No prior Java interface exists
 * (only mentioned in docs/PROJECT_STATE.md). No file I/O in this interface.
 * User instruction: implement the plan with WeVipers base http://2.28.204.253:8080.
 */
public interface DocumentVerificationPort {

    LivenessResult documentLiveness(String videoBase64);

    LivenessResult faceLiveness(String videoBase64);

    /**
     * OCR for a document kind (e.g. KENYAN_ID, KENYAN_PASSPORT).
     */
    OcrResult extract(String documentKind, String frontImageBase64, String backImageBase64);

    FaceMatchResult match(String idPortraitBase64, String selfieBase64);
}
