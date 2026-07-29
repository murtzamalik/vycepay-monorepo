# VycePay Mobile API Contract

Mobile app must call the **BFF (Backend-for-Frontend)** only. Do not call backend service ports directly.

## Base URL

- **Production:** `https://api.vycepay.com` (or your deployed BFF URL)
- **Staging / Dev:** `http://localhost:8080`

All paths below are relative to the base URL (e.g. `POST {baseUrl}/api/v1/auth/register`).

## Authentication

After signup OTP verify or successful PIN login you receive a JWT. Use it on every subsequent request.

- **Header:** `Authorization: Bearer <token>`
- Do **not** send `X-Customer-Id` from the client; the BFF sets it from the token.
- **Token TTL:** Configurable; default **10 minutes**. Use `expiresIn` (seconds) from auth responses to schedule a refresh call.
- **Token refresh:** `POST /api/v1/auth/refresh-token` — returns a new token without re-login (valid existing token required).

### Public endpoints (no Bearer token)

- `POST /api/v1/auth/register` — signup OTP send
- `POST /api/v1/auth/verify-otp` — signup OTP verify (binds IMEI + optional FCM)
- `POST /api/v1/auth/login` — PIN login (username or mobile + PIN + IMEI)
- `POST /api/v1/auth/verify-device-otp` — bind new device (no JWT; return to login)
- `POST /api/v1/auth/verify-migrate-otp` — existing user without PIN
- `POST /api/v1/auth/forgot-pin/request`
- `POST /api/v1/auth/forgot-pin/confirm`

Authenticated auth endpoints: `POST /api/v1/auth/credentials`, `POST /api/v1/auth/change-pin`.

All other `/api/v1/**` endpoints require a valid Bearer token. Missing or invalid token returns **401 Unauthorized**.

## Headers

| Header | When | Required |
|--------|------|----------|
| `Authorization: Bearer <token>` | All requests except public auth endpoints above | Yes (for protected endpoints) |
| `Content-Type: application/json` | Request has a JSON body | Yes |
| `Idempotency-Key` | `POST /api/v1/transactions/send` | Yes |
| `Idempotency-Key` | `POST /api/v1/transactions/deposit/mpesa` | Optional; when provided, duplicate requests return the same deposit |

## Success envelope (action endpoints)

Action endpoints return this shape (instead of empty body):

```json
{
  "success": true,
  "code": "AUTH_OTP_SENT",
  "message": "OTP sent successfully.",
  "requestId": "correlation-id",
  "data": null
}
```

- Use `code` for client flow logic.
- Show `message` to users where appropriate.
- Use `requestId` for support/debug traces.

## Error envelope

All errors use this shape:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "requestId": "correlation-id",
  "details": null
}
```

- Show **message** to the user.
- Use **code** for client logic (e.g. retry, redirect to KYC).
- Use **requestId** when contacting support.

## Flows (step order)

### Registration (signup)

1. `POST /api/v1/auth/register` — send SIGNUP OTP (`AUTH_OTP_SENT`).
2. `POST /api/v1/auth/verify-otp` — body: `mobileCountryCode`, `mobile`, `otpCode`, **`imei` (required)**, optional `fcmToken`, `platform`. Creates customer, binds single device IMEI, optional FCM. Returns JWT in `data`.
3. Complete KYC screens; collect username + PIN on UI (**do not** call `/auth/credentials` on signup).
4. `POST /api/v1/kyc/submit` — include KYC fields **plus required** `username` and `pin` (app login). Backend sets credentials in the same transaction as Choice submit.

### Login (PIN + single device)

1. `POST /api/v1/auth/login` — body: `username` **or** (`mobileCountryCode`+`mobile`), `pin`, **`imei`**, optional `fcmToken`/`platform`.
2. Outcomes in `data`:
   - **JWT** (`AUTH_LOGIN_OK`) — PIN ok and IMEI matches bound device; FCM replaced if provided.
   - **`deviceOtpRequired: true`** (`AUTH_DEVICE_OTP_REQUIRED`) — PIN ok, device new/unbound; OTP sent. **No token.** Client opens OTP screen → `POST /api/v1/auth/verify-device-otp` → **return to login** → login again.
   - **`mustSetCredentials: true`** (`AUTH_MUST_SET_CREDENTIALS`) — existing customer without username/PIN; migrate OTP sent. Verify via `verify-migrate-otp`, then `POST /credentials`, then login.

### Forgot PIN

1. `POST /api/v1/auth/forgot-pin/request` — `{ mobileCountryCode, mobile }`
2. `POST /api/v1/auth/forgot-pin/confirm` — `{ mobileCountryCode, mobile, otpCode, newPin, imei? }`

**Token refresh (before expiry):**

- `POST /api/v1/auth/refresh-token` (no body; uses current Bearer token) — returns new `token` + `expiresIn`.

**Logout:**

- `POST /api/v1/auth/logout` (no body) — success code: `AUTH_LOGOUT_OK`; client must discard the token.
- Backend clears all FCM device tokens for the customer (no separate unregister call needed).

**Profile:**

- `GET /api/v1/auth/me` — returns `externalId`, `mobileCountryCode`, `mobile`, `firstName`, `lastName`, `email`, `status`.

**Push notifications (FCM):**

- **Primary (mobile):** send optional `fcmToken` + `platform` (`ANDROID`) on signup `verify-otp` **or** successful PIN `login`. One FCM token per customer; each successful bind replaces the previous.
- **IMEI binding** is separate (`customer_device`); one IMEI per customer; device re-bind via OTP replaces previous.
- Omit `fcmToken` if unavailable — login still works; no push until a later verify includes a token.
- Logout clears push targets via `POST /logout`.
- **Optional / Postman:** `POST /api/v1/auth/devices` and `DELETE /api/v1/auth/devices/{deviceId}` remain for tooling; mobile should not use them.
- Push payload contract (backend → FCM): see [PUSH_NOTIFICATIONS.md](PUSH_NOTIFICATIONS.md).

### KYC (onboarding)

1. `GET /api/v1/kyc/status` — check `displayStatus` field: `NOT_STARTED | PENDING | APPROVED | REJECTED`.
2. `POST /api/v1/kyc/submit` — body: `firstName`, `middleName`, `lastName`, `birthday` (YYYY-MM-DD), `gender` (0=Female/1=Male), `countryCode` (default "254"), `mobile`, `idType` (101=NationalID/102=Alien/103=Passport), `idNumber`, `frontSidePhoto` (Base64 JPEG), `selfiePhoto` (Base64 JPEG), **`username`** (app login), **`pin`** (4-digit app login PIN). Optional: `address`, `kraPin`, `email`. Returns `choiceOnboardingRequestId`. Username/PIN are stored by VycePay only (not sent to Choice).
3. `POST /api/v1/kyc/send-otp?onboardingRequestId=<id from submit>` (success code: `KYC_OTP_SENT`).
4. `POST /api/v1/kyc/confirm-otp?onboardingRequestId=<id>&otpCode=<code>` (success code: `KYC_OTP_CONFIRMED`; invalid OTP returns error envelope with `INVALID_OTP`).

After step 4, wait for backend processing; wallet is created via webhook. Poll `GET /api/v1/wallets/me` until 200 (max 10 min).

### Wallet and transactions

1. `GET /api/v1/wallets/me` — get wallet (balance, choiceAccountId). Returns 404 until wallet exists after KYC.
2. `GET /api/v1/transactions/bank-codes` — list bank codes for “send money” UI.
3. **Validate account (Hakikisha):** `POST /api/v1/transactions/validate-account` with `accountId`, `accountType`, and `bankCode` when type is 4 (PesaLink). Show returned `accountName`; do not continue if error / not valid. Full guide: [MOBILE_VALIDATE_ACCOUNT_HANDOFF.md](MOBILE_VALIDATE_ACCOUNT_HANDOFF.md).
4. **Send money:** `POST /api/v1/transactions/send` with header `Idempotency-Key` (unique per attempt) and body (`payeeBankCode`, `payeeAccountId`, **`accountType` required**, `amount`, etc.). Server re-validates and overwrites payee name. If Choice Bank requires OTP:
   - `POST /api/v1/transactions/send-otp?transactionId=<externalId from send response>&otpType=SMS` (success code: `TXN_OTP_SENT`)
   - `POST /api/v1/transactions/confirm-otp?transactionId=<id>&otpCode=<code>` (success code: `TXN_OTP_CONFIRMED`; invalid OTP returns `INVALID_OTP`)
5. **Deposit (M-PESA):** `POST /api/v1/transactions/deposit/mpesa?mobile=<number>&amount=<kes>`. Optional header `Idempotency-Key` for idempotent deposit.

## Endpoints (BFF proxy)

All under base path `/api/v1/`. Callback is **not** for mobile (Choice Bank webhook only).

### Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/auth/register | Public | Send SIGNUP OTP (`AUTH_OTP_SENT`) |
| POST | /api/v1/auth/verify-otp | Public | Signup OTP verify → JWT + bind IMEI; optional FCM |
| POST | /api/v1/auth/login | Public | PIN login; may return `deviceOtpRequired` / `mustSetCredentials` |
| POST | /api/v1/auth/verify-device-otp | Public | Bind new device IMEI; **no JWT** — return to login |
| POST | /api/v1/auth/verify-migrate-otp | Public | Migrate OTP → JWT for `/credentials` |
| POST | /api/v1/auth/forgot-pin/request | Public | Send PIN_RESET OTP |
| POST | /api/v1/auth/forgot-pin/confirm | Public | Confirm new PIN |
| POST | /api/v1/auth/credentials | Required | Set username + PIN once (**migrate only**; signup uses KYC submit) |
| POST | /api/v1/auth/change-pin | Required | Change PIN (old + new) |
| GET | /api/v1/auth/me | Required | Current customer profile |
| POST | /api/v1/auth/refresh-token | Required | Issue new token |
| POST | /api/v1/auth/logout | Required | Logout; clears FCM tokens (`AUTH_LOGOUT_OK`) |
| POST | /api/v1/auth/devices | Required | Optional/legacy: register FCM token |
| DELETE | /api/v1/auth/devices/{deviceId} | Required | Optional/legacy: unregister FCM token |

### KYC

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/kyc/status | KYC status — use `displayStatus` (NOT_STARTED/PENDING/APPROVED/REJECTED) |
| POST | /api/v1/kyc/submit | Submit KYC + app username/PIN — returns `choiceOnboardingRequestId` |
| POST | /api/v1/kyc/send-otp?onboardingRequestId= | Send OTP for KYC confirmation (`KYC_OTP_SENT`) |
| POST | /api/v1/kyc/resend-otp?onboardingRequestId=&otpType=SMS | Resend OTP (`KYC_OTP_RESENT`) |
| POST | /api/v1/kyc/confirm-otp?onboardingRequestId=&otpCode= | Confirm KYC OTP (`KYC_OTP_CONFIRMED`) |

### Wallet

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/wallets/me | Returns balance + choiceAccountId (404 until KYC approved) |

### Transactions

| Method | Path | Notes |
|--------|------|-------|
| POST | /api/v1/transactions/validate-account | Hakikisha title fetch before send. See [MOBILE_VALIDATE_ACCOUNT_HANDOFF.md](MOBILE_VALIDATE_ACCOUNT_HANDOFF.md). |
| POST | /api/v1/transactions/send | Header: `Idempotency-Key` (required). Body requires `accountType`. Response includes `displayStatus`. |
| POST | /api/v1/transactions/deposit/mpesa?mobile=&amount= | Optional header: `Idempotency-Key` |
| POST | /api/v1/transactions/send-otp?transactionId=&otpType=SMS | Success code `TXN_OTP_SENT` |
| POST | /api/v1/transactions/resend-otp?transactionId=&otpType=SMS | Success code `TXN_OTP_RESENT` |
| POST | /api/v1/transactions/confirm-otp?transactionId=&otpCode= | Success code `TXN_OTP_CONFIRMED` |
| GET | /api/v1/transactions/{transactionId} | **Full transaction detail** (new) |
| GET | /api/v1/transactions/{transactionId}/status | Live status from Choice Bank |
| GET | /api/v1/transactions/bank-codes | Bank list for send money UI |
| GET | /api/v1/transactions/choice-history?startTime=&endTime=&page=&size= | Choice Bank transaction list |
| GET | /api/v1/transactions?page=0&size=20&status=&type= | Local transaction list. `displayStatus` in each item. |

### Activity

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/activity/log | Log an action (`ACTIVITY_LOGGED`) |
| GET | /api/v1/activity | Get activity history |

## Error codes

| code | HTTP status | When |
|------|-------------|------|
| UNAUTHORIZED | 401 | Missing or invalid Bearer token (BFF) |
| CUSTOMER_NOT_FOUND | 404 | Invalid or missing customer context |
| WALLET_NOT_FOUND | 404 | No wallet for customer (e.g. KYC not done) |
| KYC_NOT_FOUND | 404 | Onboarding request not found |
| VALIDATION_ERROR | 400 | Request body/query validation failed |
| INVALID_REQUEST | 400 | Bad request (e.g. invalid argument) |
| CONFLICT | 409 | Business rule conflict |
| FORBIDDEN | 403 | Access denied |
| NOT_FOUND | 404 | Unknown API path (BFF) |
| BAD_GATEWAY | 502 | Backend unreachable (BFF) |
| INTERNAL_ERROR | 500 | Server error |
| TRANSACTION_NOT_FOUND | 404 | Transaction not found or doesn't belong to customer |
| CUSTOMER_NOT_REGISTERED | 404 | Login/forgot-PIN for unregistered mobile |
| INVALID_OTP | 400 | OTP confirmation failed |
| OTP_EXPIRED | 400 | OTP expired |
| INVALID_CREDENTIALS | 401 | Wrong PIN or unknown username |
| ACCOUNT_LOCKED | 423 | Too many failed PIN attempts |
| USERNAME_TAKEN | 409 | Username already in use |
| USERNAME_INVALID | 400 | Username format invalid |
| CREDENTIALS_ALREADY_SET | 409 | Username/PIN already set |
| CREDENTIALS_NOT_SET | 409 | Credentials required / use mobile to migrate |
| CREDENTIALS_NOT_SET_USE_MOBILE | 409 | Migrate with mobile, not username |
| IMEI_REQUIRED | 400 | Device IMEI missing |
| RATE_LIMITED | 429 | Too many auth requests |

Choice Bank–specific codes may appear in message or details when the backend returns them (e.g. 12004 Invalid signature); treat as server/configuration errors and show message to user.

## Status Code Mappings

### Transaction `displayStatus`

| Raw `status` | `displayStatus` | Meaning |
|-------------|----------------|---------|
| `1` | `PENDING` | Submitted, awaiting Choice Bank processing |
| `2` | `PROCESSING` | Being processed by Choice Bank |
| `4` | `FAILED` | Failed — check `errorCode`/`errorMsg` |
| `8` | `SUCCESS` | Completed successfully |

### KYC `displayStatus`

| Raw `status` | `displayStatus` | Meaning |
|-------------|----------------|---------|
| (none) | `NOT_STARTED` | No KYC submitted yet |
| `1` | `PENDING` | Submitted, awaiting Choice Bank review |
| `7` | `APPROVED` | KYC approved, wallet created |
| other | `REJECTED` | Rejected — restart KYC flow |

## OTP

- Length: configurable (default **6 digits**)
- Expiry: configurable (default **5 minutes**)
- Show countdown timer; show "Resend" button at 0. Use `resend-otp` endpoint.

## Pagination

- Query params: `page` (0-based), `size`.
- Response: `totalElements`, `totalPages`, `content`.

## OpenAPI / Swagger

Backend services expose Swagger at their own ports. For a single contract, use the BFF base URL and the paths above; request/response bodies match the backend APIs (see existing API_CONTRACTS.md or each service’s `/v3/api-docs`).
