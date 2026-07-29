# VycePay Mobile Auth Handoff

**Audience:** Mobile team  
**Purpose:** Integrate username/PIN login + single-device binding against the VycePay BFF.  
**Base URL:** BFF only (never call auth-service ports directly). Example: `https://<bff-host>`

> KYC / wallet / transactions flows stay the same after auth. This document covers **auth only**.

---

## 1. Product rules (must follow)

| Rule | Detail |
|------|--------|
| Login | **Username OR mobile** + **4-digit PIN** |
| Signup | Keep existing onboarding steps; collect **username + PIN** on UI and send them **only** on `POST /kyc/submit` |
| Signup identity | Still phone OTP (`register` → `verify-otp`) to create account + JWT |
| Old OTP login | **Removed** — do not send PIN as OTP |
| Device policy | **One device only**; new device needs OTP; re-bind **replaces** old device |
| After device OTP | Bind only → **go back to login screen** → user logs in again (no JWT on device OTP) |
| Forgot PIN | Via SMS OTP |
| Existing users (no PIN yet) | Force set-credentials (migrate) gate |
| Credentials storage | VycePay backend only (never send app PIN to Choice Bank) |

---

## 2. Device ID (`imei`)

Send a stable device fingerprint on every signup verify and every login.

| Platform | Recommended value |
|----------|-------------------|
| Android | `Settings.Secure.ANDROID_ID` |
| iOS | Identifier for Vendor (IDFV) or your existing stable device id |

Field name in APIs: **`imei`** (string). Treat it as a device fingerprint, not a literal telephony IMEI.

Also send `platform`: `"ANDROID"` or `"IOS"` where noted.

---

## 3. Auth headers

| Header | When |
|--------|------|
| `Content-Type: application/json` | All JSON bodies |
| `Authorization: Bearer <token>` | All APIs **except** public list below |

**Public (no Bearer):**

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/verify-otp`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/verify-device-otp`
- `POST /api/v1/auth/verify-migrate-otp`
- `POST /api/v1/auth/forgot-pin/request`
- `POST /api/v1/auth/forgot-pin/confirm`

**Needs Bearer:**

- `POST /api/v1/auth/credentials` (migrate / existing users only — not signup)
- `POST /api/v1/auth/change-pin`
- `POST /api/v1/auth/refresh-token`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- All KYC / wallet / transaction APIs

Do **not** send `X-Customer-Id` from the app — BFF sets it from JWT.

---

## 4. Response envelopes

### Success (most auth actions)

```json
{
  "success": true,
  "code": "AUTH_LOGIN_OK",
  "message": "Login successful.",
  "requestId": "...",
  "data": { }
}
```

Branch UI on **`code`** and on flags inside **`data`**.

### Error

```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid credentials",
  "requestId": "...",
  "details": null
}
```

Show `message` to the user; use `code` for logic.

---

## 5. Screens / flows to implement

### A) Home → Signup | Login

Unchanged entry: user chooses Signup or Login.

---

### B) Signup (keep your current steps)

**Steps (UX same):** PersonalInfo (phone OTP) → CNIC → Face → Profile → **Username + PIN + Terms** → Submit

#### B1. Send signup OTP

`POST /api/v1/auth/register`

```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678"
}
```

- Success code: `AUTH_OTP_SENT`

#### B2. Verify signup OTP

`POST /api/v1/auth/verify-otp`

```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "otpCode": "123456",
  "imei": "<device-id>",
  "fcmToken": "<optional>",
  "platform": "ANDROID"
}
```

- **`imei` required**
- Success code: `AUTH_OTP_VERIFIED`
- `data` example:

```json
{
  "token": "<JWT>",
  "externalId": "<UUID>",
  "expiresIn": 600,
  "deviceOtpRequired": false,
  "mustSetCredentials": false
}
```

- Store `token` securely; use for all later APIs (KYC, etc.)
- This call also binds the device as the customer’s **first** device

#### B3. Username + PIN (collect on UI — no separate credentials API)

On your Pin / Terms screen, collect:

- **Username** (see rules below)
- **PIN** (exactly 4 digits) + confirm PIN
- Terms checkbox

**Do not** call `POST /api/v1/auth/credentials` during signup. Hold username + PIN in memory / local form state until KYC submit.

#### B4. KYC submit (sets credentials + starts Choice onboarding)

`POST /api/v1/kyc/submit` (Bearer required) — include all existing KYC fields **plus**:

```json
{
  "username": "jdoe",
  "pin": "1234",
  "firstName": "...",
  "lastName": "...",
  "birthday": "1990-01-15",
  "gender": 1,
  "countryCode": "254",
  "mobile": "712345678",
  "idType": "101",
  "idNumber": "...",
  "frontSidePhoto": "<Base64 JPEG>",
  "selfiePhoto": "<Base64 JPEG>"
}
```

- Backend sets username/PIN **in the same transaction** as Choice submit (rolls back if Choice fails)
- `username` / `pin` are **never** forwarded to Choice Bank
- Same username on retry is safe (idempotent); different username after credentials already set → `CREDENTIALS_ALREADY_SET`
- Back-nav before submit is safe: credentials are not stored until this call succeeds

---

### C) Login (known device)

`POST /api/v1/auth/login`

**With mobile:**

```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "pin": "1234",
  "imei": "<device-id>",
  "fcmToken": "<optional>",
  "platform": "ANDROID"
}
```

**With username:**

```json
{
  "username": "jdoe",
  "pin": "1234",
  "imei": "<device-id>",
  "fcmToken": "<optional>",
  "platform": "ANDROID"
}
```

Send **either** username **or** mobile fields (not both required).

#### Login outcomes (check `data` + `code`)

| Condition | `code` | What to do |
|-----------|--------|------------|
| PIN OK + device matched | `AUTH_LOGIN_OK` | Store JWT → Home |
| PIN OK + new/unbound device | `AUTH_DEVICE_OTP_REQUIRED` | Open Device OTP screen (**no JWT**) |
| Customer has no username/PIN yet | `AUTH_MUST_SET_CREDENTIALS` | Open Migrate OTP flow (**no JWT**) |

**Success login `data`:**

```json
{
  "token": "<JWT>",
  "externalId": "<UUID>",
  "expiresIn": 600,
  "username": "jdoe",
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "deviceOtpRequired": false,
  "mustSetCredentials": false
}
```

**New device `data` (example):**

```json
{
  "token": null,
  "externalId": "<UUID>",
  "expiresIn": 0,
  "deviceOtpRequired": true,
  "otpSent": true,
  "maskedMobile": "****5678",
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "mustSetCredentials": false
}
```

Use `mobileCountryCode` + `mobile` from this response for the next OTP verify call (important when user logged in with **username**).

---

### D) New device OTP → back to login

1. Show OTP screen (use `maskedMobile` in copy)
2. Call:

`POST /api/v1/auth/verify-device-otp`

```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "otpCode": "123456",
  "imei": "<new-device-id>",
  "platform": "ANDROID"
}
```

3. Success code: `AUTH_DEVICE_BOUND`  
4. **`data.deviceBound = true` — no JWT**  
5. Navigate **back to Login** with message: “Device verified. Please sign in again.”  
6. User enters credentials again → this time IMEI matches → `AUTH_LOGIN_OK`

---

### E) Existing users without PIN (migrate)

When login returns `mustSetCredentials: true`:

1. OTP screen (OTP already sent to registered mobile)
2. `POST /api/v1/auth/verify-migrate-otp`

```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "otpCode": "123456"
}
```

3. Response includes JWT — store it temporarily
4. Show “Set username + PIN” form
5. `POST /api/v1/auth/credentials` (Bearer) with username + pin + imei
6. Clear session / go to Login → normal PIN login

> Migrate start: user must use **mobile** on login if credentials are not set. Username-only without credentials returns error `CREDENTIALS_NOT_SET_USE_MOBILE`.

---

### F) Forgot PIN

1. User enters mobile  
2. `POST /api/v1/auth/forgot-pin/request`

```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678"
}
```

- Code: `AUTH_PIN_RESET_OTP_SENT`

3. User enters OTP + new 4-digit PIN  
4. `POST /api/v1/auth/forgot-pin/confirm`

```json
{
  "mobileCountryCode": "254",
  "mobile": "712345678",
  "otpCode": "123456",
  "newPin": "4321",
  "imei": "<optional-if-no-device-yet>",
  "platform": "ANDROID"
}
```

- Code: `AUTH_PIN_RESET_OK`  
- Then return to Login

---

### G) Change PIN (optional / settings)

`POST /api/v1/auth/change-pin` (Bearer)

```json
{
  "oldPin": "1234",
  "newPin": "5678"
}
```

- Code: `AUTH_PIN_CHANGED`

---

### H) Token refresh / logout / me

| API | Notes |
|-----|--------|
| `POST /api/v1/auth/refresh-token` | Bearer; no body; new token + `expiresIn` |
| `POST /api/v1/auth/logout` | Clears FCM tokens; discard JWT locally |
| `GET /api/v1/auth/me` | Profile |

Default JWT TTL ≈ **10 minutes** — refresh before expiry.

---

## 6. Validation rules (client-side)

### Username

- Length **3–20**
- Must match: starts with a letter; then letters, digits, `.` or `_`
- Case-insensitive uniqueness (backend)
- Reserved (rejected by backend): e.g. `admin`, `support`, `vycepay`, …

### PIN

- Exactly **4 digits**
- Never log PIN or OTP
- Send app PIN only on `POST /kyc/submit` (signup) or `/auth/credentials` (migrate) / forgot-pin — **never** to Choice Bank

---

## 7. Error codes (auth)

| code | HTTP | When |
|------|------|------|
| `INVALID_CREDENTIALS` | 401 | Wrong PIN / unknown username |
| `ACCOUNT_LOCKED` | 423 | Too many failed PIN attempts (lock ~15 min after 5 fails) |
| `CUSTOMER_NOT_REGISTERED` | 404 | Mobile not registered |
| `INVALID_OTP` | 400 | Bad OTP |
| `OTP_EXPIRED` | 400 | OTP expired |
| `IMEI_REQUIRED` | 400 | Missing `imei` |
| `USERNAME_TAKEN` | 409 | Username already used |
| `USERNAME_INVALID` | 400 | Bad username format |
| `CREDENTIALS_REQUIRED` | 400 | KYC submit missing username/PIN |
| `CREDENTIALS_ALREADY_SET` | 409 | Credentials already set (different username) |
| `CREDENTIALS_NOT_SET` | 409 | Credentials missing |
| `CREDENTIALS_NOT_SET_USE_MOBILE` | 409 | Migrate via mobile, not username |
| `RATE_LIMITED` | 429 | Too many requests — show retry later |
| `UNAUTHORIZED` | 401 | Missing/invalid Bearer (BFF) |

---

## 8. Success codes cheat sheet

| code | Meaning |
|------|---------|
| `AUTH_OTP_SENT` | Signup OTP sent |
| `AUTH_OTP_VERIFIED` | Signup verified + JWT |
| `AUTH_LOGIN_OK` | PIN login success + JWT |
| `AUTH_DEVICE_OTP_REQUIRED` | Need device OTP |
| `AUTH_DEVICE_BOUND` | Device bound; go to login |
| `AUTH_MUST_SET_CREDENTIALS` | Migrate flow |
| `AUTH_MIGRATE_OTP_VERIFIED` | Migrate OTP OK + JWT |
| `AUTH_CREDENTIALS_SET` | Username/PIN saved |
| `AUTH_PIN_RESET_OTP_SENT` | Forgot-PIN OTP sent |
| `AUTH_PIN_RESET_OK` | PIN reset done |
| `AUTH_PIN_CHANGED` | PIN changed |
| `AUTH_LOGOUT_OK` | Logout done |

---

## 9. Mobile checklist (delivery)

- [ ] Login screen: identifier (username **or** phone) + 4-digit PIN; always send `imei`
- [ ] Remove old “login = send OTP / PIN as otpCode” flow
- [ ] Handle `deviceOtpRequired` → OTP screen → verify-device-otp → **back to login**
- [ ] Handle `mustSetCredentials` → migrate OTP → set credentials → login
- [ ] Forgot PIN screens wired to request/confirm
- [ ] Signup: `register` → `verify-otp` (with `imei`) → store JWT
- [ ] Signup Pin step: collect username + PIN locally → include on `POST /kyc/submit` (do **not** call `/auth/credentials` on signup)
- [ ] Secure token storage + refresh before expiry
- [ ] Optional FCM on signup verify-otp and on successful login
- [ ] Never log PIN/OTP; never send app PIN to Choice Bank (KYC submit is OK — backend strips it)
- [ ] Map error codes above to user-friendly UI

---

## 10. What did **not** change

- KYC screens / Choice Bank KYC OTP APIs (only submit body gained `username` + `pin`)
- Wallet / send money / deposit / statement APIs
- Still call **BFF only**
- Existing-user migrate still uses `POST /auth/credentials` after `verify-migrate-otp`

---

## 11. Contact / support

For API issues, include `requestId` from the error/success envelope.

Backend owner: VycePay auth-service via BFF.  
Related docs (optional deeper read): `MOBILE_API_CONTRACT.md`, `BUSINESS_LOGIC_AND_FLOWS.md`.
