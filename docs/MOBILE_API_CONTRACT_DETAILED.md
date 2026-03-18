# VycePay Mobile API Contract (Detailed)

This contract is for the **mobile team** and any downstream AI tooling.
Mobile apps must call the **BFF only** (Backend-for-Frontend). Do not call backend service ports directly.

---

## Base URL

- **Production:** your deployed BFF URL (example: `https://api.vycepay.com`)
- **Local / dev (single-container docker):** `http://localhost:9080`

All paths below are relative to the base URL.
Example: `POST /api/v1/auth/register` means `POST {baseUrl}/api/v1/auth/register`.

---

## Authentication and Authorization

### Bearer token

After registration/login, the client receives a JWT (`token`). Send it on every protected request:

- Header: `Authorization: Bearer <token>`

The BFF derives customer identity from the JWT and injects `X-Customer-Id` into downstream calls.
The client must **not** send `X-Customer-Id`.

### Public endpoints (no Bearer token required)

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/verify-otp`

All other `/api/v1/**` endpoints require a valid Bearer token.
Missing or invalid token returns `401 Unauthorized`.

---

## Idempotency-Key

Headers:

- `Idempotency-Key` on `POST /api/v1/transactions/send` is **required**
- `Idempotency-Key` on `POST /api/v1/transactions/deposit/mpesa` is **optional**

---

## Content-Type

- For JSON request bodies: `Content-Type: application/json`

---

## Error Envelope

For errors, the BFF returns this shape:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "requestId": "correlation-id",
  "details": null
}
```

`code` is for client logic; `requestId` is for support/debugging.

---

## Auth APIs

### 1) Register (send OTP)

- **POST** `/api/v1/auth/register`
- **Auth:** Public
- **Request (JSON):**
```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678"
}
```
- **Response:** `200 OK` (empty body)

### 2) Login (send OTP)

- **POST** `/api/v1/auth/login`
- **Auth:** Public
- **Request (JSON):** same as register
```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678"
}
```
- **Response:** `200 OK` (empty body)

### 3) Verify OTP (get JWT)

- **POST** `/api/v1/auth/verify-otp`
- **Auth:** Public
- **Request (JSON):**
```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "otpCode": "123456"
}
```
- **Response (200, JSON):**
```json
{
  "token": "<JWT>",
  "externalId": "<UUID>",
  "expiresIn": 600
}
```

### 4) Refresh token

- **POST** `/api/v1/auth/refresh-token`
- **Auth:** Required (Bearer)
- **Request body:** none
- **Response (200, JSON):**
```json
{
  "token": "<new JWT>",
  "externalId": "<UUID>",
  "expiresIn": 600
}
```

### 5) Logout

- **POST** `/api/v1/auth/logout`
- **Auth:** Required (Bearer)
- **Request body:** none
- **Response:** `200 OK` (empty body)
- **Client behavior:** discard token

### 6) Get profile

- **GET** `/api/v1/auth/me`
- **Auth:** Required (Bearer)
- **Response (200, JSON):**
```json
{
  "externalId": "<UUID>",
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "firstName": "Test",
  "lastName": "User",
  "email": null,
  "status": "ACTIVE"
}
```

### 7) Register device for push

- **POST** `/api/v1/auth/devices`
- **Auth:** Required (Bearer)
- **Request (JSON):**
```json
{
  "fcmToken": "<firebase-token>",
  "platform": "ANDROID"
}
```
- **Response:** `200 OK` (empty body)

### 8) Unregister device for push

- **DELETE** `/api/v1/auth/devices/{deviceId}`
- **Auth:** Required (Bearer)
- **Response:** `200 OK` (empty body)

---

## KYC APIs (Choice Bank onboarding)

### 9) Get KYC status

- **GET** `/api/v1/kyc/status`
- **Auth:** Required (Bearer)
- **Response (200, JSON):**
```json
{
  "status": "1",
  "displayStatus": "PENDING",
  "choiceOnboardingRequestId": "ONBRD....",
  "choiceAccountId": null
}
```

`displayStatus` mapping:
- `NOT_STARTED`
- `PENDING`
- `APPROVED`
- `REJECTED`

### 10) Submit KYC to Choice Bank

- **POST** `/api/v1/kyc/submit`
- **Auth:** Required (Bearer)
- **Request (JSON):**
```json
{
  "firstName": "Test",
  "middleName": "User",
  "lastName": "Sandbox",
  "birthday": "1990-01-15",
  "gender": 1,
  "countryCode": "254",
  "mobile": "712345678",
  "idType": "101",
  "idNumber": "12345678",
  "frontSidePhoto": "<Base64 JPEG>",
  "backSidePhoto": "<Base64 JPEG>",
  "selfiePhoto": "<Base64 JPEG>",
  "address": null,
  "kraPin": null,
  "email": null
}
```
- **Response (200, JSON):**
```json
{
  "status": "1",
  "displayStatus": "PENDING",
  "choiceOnboardingRequestId": "ONBRD....",
  "choiceAccountId": null
}
```

### 11) Send KYC OTP

- **POST** `/api/v1/kyc/send-otp?onboardingRequestId=<id>`
- **Auth:** Required (Bearer)
- **Request body:** none
- **Response:** `200 OK` (empty body)

### 12) Resend KYC OTP

- **POST** `/api/v1/kyc/resend-otp?onboardingRequestId=<id>&otpType=SMS`
- **Auth:** Required (Bearer)
- **Response:** `200 OK` (empty body)

### 13) Confirm KYC OTP

- **POST** `/api/v1/kyc/confirm-otp?onboardingRequestId=<id>&otpCode=<code>`
- **Auth:** Required (Bearer)
- **Response:** `200 OK` (empty body) on success, `400` on failure (empty body)

### After KYC confirm

Choice processing is asynchronous.
Wallet is created when the callback indicates account opened.
Client should poll:

- `GET /api/v1/wallets/me`
until it returns `200` (wallet doesn’t exist yet => `404` while pending).

---

## Wallet APIs

### 14) Get current wallet

- **GET** `/api/v1/wallets/me`
- **Auth:** Required (Bearer)
- **Response (200, JSON):**
```json
{
  "choiceAccountId": "....",
  "balance": 0.00,
  "currency": "KES",
  "status": "ACTIVE"
}
```
- **Response:** `404` if wallet not created yet

---

## Transaction APIs

### 15) Send money (transfer)

- **POST** `/api/v1/transactions/send`
- **Auth:** Required (Bearer)
- **Headers:**
  - `Idempotency-Key` (required)
- **Request (JSON):**
```json
{
  "payeeBankCode": "M-PESA",
  "payeeAccountId": "712345678",
  "payeeAccountName": "Recipient Name",
  "amount": 100.50,
  "remark": "Reference (optional)"
}
```
- **Response:** `200 OK` with `TransactionResponse` JSON (fields below)

If OTP is required by Choice Bank:
- `POST /api/v1/transactions/send-otp?transactionId=<externalId>&otpType=SMS`
- `POST /api/v1/transactions/confirm-otp?transactionId=<externalId>&otpCode=<code>`

### 16) Deposit via M-PESA STK push

- **POST** `/api/v1/transactions/deposit/mpesa?mobile=<number>&amount=<kes>`
- **Auth:** Required (Bearer)
- **Headers:**
  - `Idempotency-Key` (optional)
- **Body:** none
- **Response:** `200 OK` with `TransactionResponse` JSON

### 17) Transaction OTP endpoints

- **POST** `/api/v1/transactions/send-otp?transactionId=<externalId>&otpType=SMS`
- **POST** `/api/v1/transactions/resend-otp?transactionId=<externalId>&otpType=SMS`
- **POST** `/api/v1/transactions/confirm-otp?transactionId=<externalId>&otpCode=<code>`

Confirm OTP:
- `200 OK` on success
- `400` on failure (empty body)

### 18) Full transaction detail

- **GET** `/api/v1/transactions/{transactionId}`
- **Auth:** Required (Bearer)
- `transactionId` is the **externalId (UUID)** returned by `POST /api/v1/transactions/send`
- **Response (200, JSON):** `TransactionResponse`

### 19) Transaction status (Choice query)

- **GET** `/api/v1/transactions/{transactionId}/status`
- **Auth:** Required (Bearer)
- **Response (200, JSON):** a Choice status payload mapped as a `Map<String,Object>`

### 20) Bank codes (for UI selection)

- **GET** `/api/v1/transactions/bank-codes`
- **Auth:** Required (Bearer)
- **Response:** `200 OK` with a JSON object (`Map<String,Object>`) from Choice

### 21) Choice transaction history

- **GET** `/api/v1/transactions/choice-history?startTime=<ms>&endTime=<ms>&page=<1-based>&size=<pageSize>`
- **Auth:** Required (Bearer)
- **Response:** `200 OK` with JSON object (`Map<String,Object>`) from Choice

### 22) Local transaction list (paginated)

- **GET** `/api/v1/transactions?page=0&size=20&status=&type=`
- **Auth:** Required (Bearer)
- Optional query params:
  - `status`
  - `type`
- **Response (200, JSON):** Spring `Page<TransactionResponse>` serialized as a page object

---

### TransactionResponse schema (used by send/deposit/detail endpoints)

```json
{
  "externalId": "<UUID>",
  "type": "TRANSFER",
  "amount": 100.50,
  "currency": "KES",
  "status": "1",
  "displayStatus": "PENDING",
  "payeeAccountId": "712345678",
  "payeeAccountName": "Recipient Name",
  "payeeBankCode": "M-PESA",
  "remark": "Reference",
  "createdAt": "2026-03-18T10:00:00Z",
  "completedAt": null
}
```

`displayStatus` mapping:
- raw `status` = `1` → `PENDING`
- raw `status` = `2` → `PROCESSING`
- raw `status` = `4` → `FAILED`
- raw `status` = `8` → `SUCCESS`

---

## Activity APIs

### 23) Log an activity

- **POST** `/api/v1/activity/log`
- **Auth:** Required (Bearer)
- **Request (JSON):**
```json
{
  "action": "LOGIN",
  "resourceType": "TRANSACTION",
  "resourceId": "....",
  "ipAddress": "1.2.3.4",
  "userAgent": "App/1.0",
  "deviceId": "device-123"
}
```
- **Response:** `200 OK` (empty body)

### 24) List activity

- **GET** `/api/v1/activity?page=0&size=20`
- **Auth:** Required (Bearer)
- **Response:** `200 OK` with a paginated list of maps (Spring Page)
- Each entry includes:
  - `id`, `action`, `resourceType`, `resourceId`, `createdAt` (string)

