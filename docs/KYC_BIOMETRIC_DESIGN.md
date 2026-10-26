# KYC Biometric Design (implemented)

## Summary

VycePay orchestrates WeVipers biometric APIs (OCR, optional document liveness, face liveness, face match), stores media, audits attempts, and gates Choice submit behind `vycepay.kyc.biometric.required` (default false).

**WeVipers base URL (server only):** `http://2.28.204.253:8080` (`WEVIPERS_BASE_URL`)

## Locked decisions

- Document liveness optional
- Submit photos: reuse session media; body override allowed
- No server-side cropping
- Media retention: configure S3 lifecycle ~7 years; local path for default
- Feature flag default off until Android cutover

## Mobile

See [MOBILE_KYC_BIOMETRIC_HANDOFF.md](MOBILE_KYC_BIOMETRIC_HANDOFF.md)

## Admin

- KYC detail: summary + document gallery + biometric timeline
- Document stream: `GET /api/admin/v1/kyc/{id}/documents/{id-front|id-back|id-portrait|selfie}` (`kyc:view_documents`)
- List columns: biometricStatus, biometricFailCode

## Schema

Flyway `V16__kyc_biometric.sql` — `kyc_biometric_session`, `kyc_biometric_attempt`, alter `kyc_verification`.

## Rollout

1. Deploy with `KYC_BIOMETRIC_REQUIRED=false`
2. Ship Android against VycePay biometric APIs
3. Set `KYC_BIOMETRIC_REQUIRED=true`
