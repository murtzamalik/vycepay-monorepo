# Mobile KYC Biometric Handoff

**WeVipers base URL is NEVER called from the app.** All calls go through VycePay BFF → kyc-service.  
Vendor (server-side only): `http://2.28.204.253:8080`

## Flow

1. Auth OTP → JWT  
2. `POST /api/v1/kyc/biometric/sessions` `{ idType, documentCountry, forceNew? }`  
3. Optional: `POST .../document-liveness` `{ videoBase64 }`  
4. `POST .../ocr` `{ documentKind, frontImageBase64, backImageBase64? }`  
5. `POST .../face-liveness` `{ videoBase64 }`  
6. `POST .../face-match` `{ idPortraitBase64, selfieBase64 }` (mobile crops portrait; VycePay pass-through)  
7. When `canSubmit=true` / `READY_TO_SUBMIT`: `POST /api/v1/kyc/submit` with `biometricSessionId` (+ optional photo override Base64)  
8. Existing Choice OTP flow unchanged  

`GET /api/v1/kyc/status` now may include `biometricSessionId`, `biometricStatus`, `biometricCanSubmit`.

## Feature flag

`vycepay.kyc.biometric.required` / env `KYC_BIOMETRIC_REQUIRED` default **false**.  
When **true**, submit without READY session returns `KYC_BIOMETRIC_REQUIRED` / `KYC_BIOMETRIC_NOT_READY`.

## Timeouts

BFF read timeout is **130s**. Client should use ≥120s for liveness, ≥60s OCR, ≥30s match.

## Cutover

1. Backend deployed with required=false  
2. App removes direct WeVipers calls  
3. Flip required=true after adoption  
