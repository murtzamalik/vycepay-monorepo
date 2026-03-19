# VycePay Mobile API Contract

Mobile app must call the **BFF (Backend-for-Frontend)** only. Do not call backend service ports directly.

## Base URL

- **Production:** `https://api.vycepay.com` (or your deployed BFF URL)
- **Staging / Dev:** `http://localhost:8080`

All paths below are relative to the base URL (e.g. `POST {baseUrl}/api/v1/auth/register`).

## Authentication

After login or registration you receive a JWT in the verify-otp response. Use it on every subsequent request.

- **Header:** `Authorization: Bearer <token>`
- Do **not** send `X-Customer-Id` from the client; the BFF sets it from the token.
- **Token TTL:** Configurable; default **10 minutes**. Use `expiresIn` (seconds) from the verify-otp response to schedule a refresh call.
- **Token refresh:** `POST /api/v1/auth/refresh-token` — returns a new token without re-sending OTP (valid existing token required).

### Public endpoints (no Bearer token)

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/verify-otp`

All other `/api/v1/**` endpoints require a valid Bearer token. Missing or invalid token returns **401 Unauthorized**.

## Headers

| Header | When | Required |
|--------|------|----------|
| `Authorization: Bearer <token>` | All requests except the three auth endpoints above | Yes (for protected endpoints) |
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

### Registration and login

1. `POST /api/v1/auth/register` — send OTP (success code: `AUTH_OTP_SENT`).
2. `POST /api/v1/auth/verify-otp` — verify OTP (body: `mobileCountryCode`, `mobile`, `otpCode`). Response: `token`, `externalId`, `expiresIn` (seconds). Store token for all later requests.

**Returning user:**

1. `POST /api/v1/auth/login` — send OTP (success code: `AUTH_LOGIN_OTP_SENT`).
2. `POST /api/v1/auth/verify-otp` — same as above; get new token.

**Token refresh (before expiry):**

- `POST /api/v1/auth/refresh-token` (no body; uses current Bearer token) — returns new `token` + `expiresIn`.

**Logout:**

- `POST /api/v1/auth/logout` (no body) — success code: `AUTH_LOGOUT_OK`; client must discard the token.
- Also call `DELETE /api/v1/auth/devices/{deviceId}` to unregister FCM token.

**Profile:**

- `GET /api/v1/auth/me` — returns `externalId`, `mobileCountryCode`, `mobile`, `firstName`, `lastName`, `email`, `status`.

**Push notifications (FCM):**

- `POST /api/v1/auth/devices` — body: `{ "fcmToken": "...", "platform": "ANDROID" }`, success code: `DEVICE_REGISTERED`.
- `DELETE /api/v1/auth/devices/{deviceId}` — success code: `DEVICE_UNREGISTERED`.

### KYC (onboarding)

1. `GET /api/v1/kyc/status` — check `displayStatus` field: `NOT_STARTED | PENDING | APPROVED | REJECTED`.
2. `POST /api/v1/kyc/submit` — body: `firstName`, `middleName`, `lastName`, `birthday` (YYYY-MM-DD), `gender` (0=Female/1=Male), `countryCode` (default "254"), `mobile`, `idType` (101=NationalID/102=Alien/103=Passport), `idNumber`, `frontSidePhoto` (Base64 JPEG), `selfiePhoto` (Base64 JPEG). Optional: `address`, `kraPin`, `email`. Returns `choiceOnboardingRequestId`.
3. `POST /api/v1/kyc/send-otp?onboardingRequestId=<id from submit>` (success code: `KYC_OTP_SENT`).
4. `POST /api/v1/kyc/confirm-otp?onboardingRequestId=<id>&otpCode=<code>` (success code: `KYC_OTP_CONFIRMED`; invalid OTP returns error envelope with `INVALID_OTP`).

After step 4, wait for backend processing; wallet is created via webhook. Poll `GET /api/v1/wallets/me` until 200 (max 10 min).

### Wallet and transactions

1. `GET /api/v1/wallets/me` — get wallet (balance, choiceAccountId). Returns 404 until wallet exists after KYC.
2. `GET /api/v1/transactions/bank-codes` — list bank codes for “send money” UI.
3. **Send money:** `POST /api/v1/transactions/send` with header `Idempotency-Key` (unique per attempt) and body (payeeBankCode, payeeAccountId, amount, etc.). If Choice Bank requires OTP:
   - `POST /api/v1/transactions/send-otp?transactionId=<externalId from send response>&otpType=SMS` (success code: `TXN_OTP_SENT`)
   - `POST /api/v1/transactions/confirm-otp?transactionId=<id>&otpCode=<code>` (success code: `TXN_OTP_CONFIRMED`; invalid OTP returns `INVALID_OTP`)
4. **Deposit (M-PESA):** `POST /api/v1/transactions/deposit/mpesa?mobile=<number>&amount=<kes>`. Optional header `Idempotency-Key` for idempotent deposit.

## Endpoints (BFF proxy)

All under base path `/api/v1/`. Callback is **not** for mobile (Choice Bank webhook only).

### Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/auth/register | Public | Send OTP for registration (`AUTH_OTP_SENT`) |
| POST | /api/v1/auth/login | Public | Send OTP for login (`AUTH_LOGIN_OTP_SENT`) |
| POST | /api/v1/auth/verify-otp | Public | Verify OTP → returns `token`, `externalId`, `expiresIn` |
| GET | /api/v1/auth/me | Required | Current customer profile |
| POST | /api/v1/auth/refresh-token | Required | Issue new token (no OTP needed) |
| POST | /api/v1/auth/logout | Required | Acknowledge logout (`AUTH_LOGOUT_OK`) |
| POST | /api/v1/auth/devices | Required | Register FCM token (`DEVICE_REGISTERED`) |
| DELETE | /api/v1/auth/devices/{deviceId} | Required | Unregister FCM token (`DEVICE_UNREGISTERED`) |

### KYC

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/kyc/status | KYC status — use `displayStatus` (NOT_STARTED/PENDING/APPROVED/REJECTED) |
| POST | /api/v1/kyc/submit | Submit KYC data to Choice Bank — returns `choiceOnboardingRequestId` |
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
| POST | /api/v1/transactions/send | Header: `Idempotency-Key` (required). Response includes `displayStatus`. |
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
| CUSTOMER_NOT_REGISTERED | 404 | Login attempted for unregistered mobile |
| INVALID_OTP | 400 | OTP confirmation failed |

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
