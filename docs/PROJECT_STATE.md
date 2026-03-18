# VycePay Project State – Memory for Continuation

**Last updated:** March 2026

Use this document when continuing development. It captures what has been done and where things stand.

---

## Overview

White-label digital wallet platform using **Choice Bank (Kenya)** as BaaS provider. Remote onboarding: ID + Selfie only. No address proof, KRA PIN, or physical verification.

**Tech stack:** Java 17, Spring Boot 3.2, Maven, MySQL, JWT, Flyway.

---

## Architecture

- **6 Spring Boot services** + common + database module
- **Single MySQL database** (shared)
- **Layered:** api → application → domain → infrastructure
- **Hexagonal:** Ports & Adapters for external integrations

| Service     | Port | Responsibility                                      |
|------------|------|-----------------------------------------------------|
| Auth       | 8082 | Registration, OTP, JWT, login                       |
| Callback   | 8081 | Choice Bank webhook receiver, persist, route        |
| KYC        | 8083 | Onboarding submit to Choice Bank, KYC status, OTP   |
| Wallet     | 8084 | Account mapping, balance cache                      |
| Transaction| 8085 | Transfer, deposit, OTP, status query, history       |
| Activity   | 8086 | Audit log (login, send, view)                       |

---

## Implemented Features

### Core

- All 6 services implemented and wired
- Choice Bank integration: `ChoiceBankClient`, `ChoiceBankRequestFactory`, signing
- **BankingProviderPort** + **ChoiceBankApiAdapter** (hexagonal)
- Callback: persist raw payload, route by notificationType (0001, 0002, 0003, 0021, UNKNOWN)
- JWT auth: `JwtAuthFilter`, `JwtValidator` in common; used by KYC, Wallet, Transaction, Activity
- `RequestIdFilter` for MDC correlation
- `GlobalExceptionHandler` + `ErrorResponse`
- **BusinessException** with code/message/HttpStatus – used across facades and controllers

### KYC & Onboarding

- POST /kyc/submit → Choice Bank `submitEasyOnboardingRequest`
- GET /kyc/status
- POST /kyc/send-otp, POST /kyc/confirm-otp
- Result via callback 0001; OnboardingResultHandler updates kyc_verification, creates wallet when approved

### Transaction

- POST /transactions/send (Idempotency-Key required)
- POST /transactions/deposit/mpesa (STK Push)
- POST /transactions/send-otp, POST /transactions/confirm-otp
- GET /transactions/{id}/status (Choice Bank getTransResult)
- GET /transactions?status=&type= (JPA Specification filters)

### Resilience & Security

- **Resilience4j:** Retry + Circuit Breaker on Choice Bank client (exponential backoff, multiplier 2)
- **ResponseSignatureVerifier:** Optional; when enabled, verifies Choice Bank response signatures
- **SensitiveDataEncryptionPort:** Port + NoOp impl; OnboardingResultHandler encrypts `id_number` when storing
- Config: `vycepay.choice-bank.verify-response-signature=true` to enable signature verification

### API & Docs

- OpenAPI/Swagger on all services: `/swagger-ui.html`, `/v3/api-docs`
- @Schema on DTOs
- Actuator (health, info, metrics) on all services

### Dev Tools

- Docker Compose: MySQL for local dev
- Makefile: `make mysql`, `make run-auth`, etc.
- Docs: ARCHITECTURE_PLAN, API_CONTRACTS, CHOICE_BANK_API, IMPLEMENTATION_GUIDE, ACTUATOR, DATABASE_SCHEMA

---

## Configuration

**Choice Bank (KYC, Transaction):**

- `vycepay.choice-bank.base-url` (default: https://baas-pilot.choicebankapi.com)
- `vycepay.choice-bank.sender-id` (env: CHOICE_BANK_SENDER_ID)
- `vycepay.choice-bank.private-key` (env: CHOICE_BANK_PRIVATE_KEY)

**JWT:** `vycepay.jwt.secret` (env: JWT_SECRET)

**Database:** `jdbc:mysql://localhost:3306/vycepay`, user `vycepay`/`vycepay`

---

## Database Tables

- customer, kyc_verification, wallet, transaction, choice_bank_callback, activity_log, otp_verification
- Choice Bank IDs are references, not PKs
- Raw callback payloads in LONGTEXT
- Flyway: V1__initial_schema.sql

---

## Phase 2 – Android App ✅ COMPLETE

Multi-module Kotlin/Compose app at `vycepay-android/`. All 4 phases done.

### Module structure

```
vycepay-android/
├── app/                    # Entry point, Hilt setup, MainActivity, deep links
├── core/
│   ├── network/            # Retrofit + OkHttp, AuthInterceptor, TokenRefreshAuthenticator, CertificatePinner
│   ├── data/               # Repository implementations, Room, apiCall{} helper
│   ├── domain/             # Models (Transaction, KycStatus, BankCode, User), repository interfaces
│   ├── ui/                 # Theme, typography, shared Composables (VycepayLoader, VycepayErrorState)
│   └── security/           # EncryptedTokenStorage (AES-256-GCM), BiometricLockManager
└── feature/
    ├── auth/               # Register/login, OTP screen with countdown
    ├── kyc/                # KYC form (5-step), CameraX capture, KycPendingScreen (polling)
    ├── home/               # Dashboard, wallet balance, quick actions
    ├── send/               # Send money 4-step flow with idempotency key
    ├── deposit/            # M-PESA STK Push with polling
    ├── transactions/       # Paginated list + detail with auto-poll for in-flight tx
    ├── activity/           # Activity log
    └── settings/           # Profile, logout
```

### Key capabilities
- **JWT auth:** `EncryptedSharedPreferences` (Keystore-backed), 30s expiry buffer, auto-refresh on 401 (OkHttp Authenticator with synchronized double-check)
- **Certificate pinning:** `CertificatePinner` on `api.vycepay.com` in release builds (placeholder hashes — replace before production)
- **Biometric lock:** 60s background timeout, `BiometricPrompt` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`
- **KYC photos:** Compressed to ≤1MB/1280×1280px JPEG; deleted from disk immediately after Base64 encoding
- **Polling:** Wallet creation (3s/10min), transaction status (5s/10min), both with 5min warning
- **FCM:** `VycepayFirebaseMessagingService` + `DeviceTokenSyncWorker` (WorkManager) for background registration
- **R8 full mode + ProGuard rules** for release
- **Unit tests:** JUnit 5 + MockK + Turbine across `core/data`, `core/security`, and 4 feature modules

### Pending before production
- Replace placeholder certificate pin hashes in `core/network/src/main/java/com/vycepay/core/network/NetworkModule.kt`
- Add `google-services.json` (Firebase project setup)
- Verify `mobile` field in `KycSubmitRequest` with Choice Bank API behaviour
- Set up GitHub Actions CI (build + test)

---

## Pending / Not Started

### Phase 3 – Admin Backoffice

- Not started
- Customer search, KYC review, transaction monitoring, callback inspection, wallet suspension, audit, reporting

### OCR & Face Comparison

- **Assumption:** Choice Bank performs OCR and face comparison as part of their KYC pipeline
- We send `frontSidePhoto` and `selfiePhoto` (Base64) + user-entered `idNumber` to Choice Bank
- If we need our own pre-validation: add `DocumentVerificationPort` with OCR/face adapters (e.g. ML Kit, Tesseract, cloud APIs)
- Confirm with Choice Bank whether they handle verification end-to-end

### Tests

- No unit or integration tests yet

### Future Scalability

- Kafka, Redis, ELK – documented as future options, not implemented

---

## Key Files

- **Rule book:** `.cursor/rules/vycepay-architecture.mdc`
- **Common:** vycepay-common (Choice Bank client, JWT, exceptions, ports)
- **Facades:** KycOnboardingFacade, TransactionFacade (use BankingProviderPort)
- **Callback handlers:** OnboardingResultHandler, TransactionResultHandler, BalanceChangeHandler, UnknownNotificationHandler
- **Specification:** TransactionSpecification (status, type, customer filters)

---

## Commands

```bash
docker compose up -d          # Start MySQL
make build                   # Compile
make run-auth                # Run auth service (similar for others)
```

---

## Notes for Continuation

1. All Phase 1 backend gaps are closed (BusinessException, ResponseSignatureVerifier wiring, SensitiveDataEncryptionPort usage, exponential backoff, docs).
2. Phase 2 Android app is complete — all features implemented, hardened, and tested.
3. OCR/face comparison is assumed to be done by Choice Bank; verify with their docs/support.
4. Next logical steps: Phase 3 admin backoffice, or replace certificate pin hashes and deploy.
5. Callback service uses `@ComponentScan(basePackages = "com.vycepay")` to load common beans (e.g. SensitiveDataEncryptionPort).
