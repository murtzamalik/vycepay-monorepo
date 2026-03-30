# VycePay — Technical Reference

Quick reference for ports, config, key classes, and schema. Use with ARCHITECTURE_DETAILED and BUSINESS_LOGIC_AND_FLOWS for full context.

---

## 1. Ports

| Service | Default port | Notes |
|---------|-------------|--------|
| BFF | 8080 | In Docker, host often 9080 |
| Callback | 8081 | Choice webhook |
| Auth | 8082 | |
| KYC | 8083 | |
| Wallet | 8084 | |
| Transaction | 8085 | |
| Activity | 8086 | |

Port is overridden by env `PORT` (e.g. in docker-entrypoint).

---

## 2. Configuration (Environment / application.yml)

| Key | Where | Purpose |
|-----|--------|---------|
| `SPRING_DATASOURCE_URL` | All services | JDBC URL, e.g. `jdbc:mysql://127.0.0.1:3306/vycepay?useSSL=false&serverTimezone=UTC` |
| `DB_USERNAME` / `DB_PASSWORD` | All services | DB credentials |
| `JWT_SECRET` | Auth, BFF | Same secret so BFF can validate tokens issued by auth |
| `CHOICE_BANK_BASE_URL` | KYC, Transaction (via common) | Choice BaaS base URL |
| `CHOICE_BANK_SENDER_ID` | Common (Choice client) | e.g. VYCEIN |
| `CHOICE_BANK_PRIVATE_KEY` | Common | BaaS request signing |
| `vycepay.bff.auth-url`, `kyc-url`, `wallets-url`, `transactions-url`, `activity-url` | BFF | Backend base URLs (no trailing slash) |
| `vycepay.callback.verify-signature` | Callback | Optional callback signature verification |
| `vycepay.choice-bank.logging.enabled` | Common (`ChoiceBankClient`) | Log paired `choice_baas_request` / `choice_baas_response` lines (default `true`) |
| `vycepay.choice-bank.logging.log-bodies` | Common | Include JSON bodies in those logs (default `true`; set `false` for metadata only) |
| `vycepay.choice-bank.logging.redact-signatures` | Common | Redact `signature` / `salt` in logged JSON (default `true`) |

---

## 3. Key Classes and Packages

| Area | Class / Package | Role |
|------|------------------|------|
| BFF | `BffProxyController` | Proxies /api/v1/** to backends |
| BFF | `BffJwtFilter` | Validates JWT, sets X-Customer-Id |
| BFF | `BffBackendProperties` | Backend base URLs |
| Auth | `AuthFacade` | sendOtp, verifyOtpAndGetToken, login |
| Auth | `OtpService` | Generate/store OTP (otp_verification) |
| Auth | `JwtService` | Create JWT (customer id, externalId) |
| KYC | `KycOnboardingFacade` | submitOnboarding, sendOtp, resendOtp, confirmOtp, getOnboardingStatus |
| KYC | `KycController` | REST for status, submit, send-otp, resend-otp, confirm-otp |
| Wallet | Wallet controller/repository | GET wallets/me from DB |
| Transaction | `TransactionFacade` | applyTransfer, depositFromMpesa, sendTransferOtp, confirmTransferOtp, status, etc. |
| Callback | `CallbackService` | receiveAndProcess, persist, processAsync |
| Callback | `NotificationHandler` (0001, 0002, 0003, UNKNOWN) | Strategy handlers |
| Common | `ChoiceBankClient` | HTTP to Choice; uses requestFactory and signing |
| Common | `ChoiceBankRequestFactory` | Builds signed body (requestId, sender, timestamp, salt, signature, params) |
| Common | `ChoiceBankSignatureUtil` | BaaS signature (flatten, sort, sign); RequestIdGenerator (senderId + UUID no hyphens) |
| Common | `JwtValidator` | Validates JWT, returns externalId |

---

## 4. Database Tables (Summary)

| Table | Purpose |
|-------|---------|
| customer | User identity; external_id (UUID) for API |
| otp_verification | Registration OTP (auth service) |
| kyc_verification | Choice onboarding; links to callback 0001 |
| wallet | Choice account mapping; balance_cache updated by 0003 |
| transaction | Pending/completed tx; idempotency_key; choice_tx_id, choice_request_id |
| choice_bank_callback | Raw callback audit; processed flag |
| activity_log | Audit trail (action, resource, metadata) |

Full DDL: `vycepay-database/src/main/resources/db/migration/V1__initial_schema.sql`.

---

## 5. API Base Path and Error Envelope

- **Base path:** `/api/v1/`
- **Error body (typical):** `{ "code": "ERROR_CODE", "message": "Human-readable", "requestId": "...", "details": ... }`
- **Pagination:** `page` (0-based), `size`; response: `totalElements`, `totalPages`, `content`.

---

## 6. Important Conventions

- **Customer in APIs:** Identified by `externalId` (UUID from `customer.external_id`). Sent as `X-Customer-Id` by BFF.
- **Transaction id in URLs/bodies:** The **externalId** (UUID) of the `transaction` row (returned by POST /transactions/send), not Choice’s txId.
- **Idempotency:** POST /transactions/send requires header `Idempotency-Key`. POST /transactions/deposit/mpesa supports optional `Idempotency-Key`.
- **Choice requestId:** Format `{senderId}{32 hex chars}` (no hyphens), e.g. VYCEIN + UUID without hyphens.

---

## 7. Swagger / OpenAPI

- **BFF:** `/swagger-ui.html`, `/v3/api-docs` (no auth for these paths).
- **Backends:** Each service exposes Swagger on its own port (e.g. auth 8082, transaction 8085). See API_CONTRACTS.md and MOBILE_API_CONTRACT.md for endpoint lists.
