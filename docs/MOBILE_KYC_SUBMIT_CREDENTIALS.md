# Mobile Team — Username / PIN on KYC Submit

**Audience:** Mobile (Android / iOS)  
**Date:** 2026-07-29  
**Status:** Backend deployed on `main` (`bbd0264`) — app must update before/with this deploy  
**Full auth reference:** [MOBILE_AUTH_HANDOFF.md](MOBILE_AUTH_HANDOFF.md)

---

## What changed (read this first)

| Before (broken) | After (required) |
|-----------------|------------------|
| Pin screen → `POST /auth/credentials` → then `POST /kyc/submit` | Pin screen → **hold username+PIN in app** → `POST /kyc/submit` **with** `username` + `pin` |
| Back from submit → credentials already in DB → `CREDENTIALS_ALREADY_SET` | Back before submit is safe — credentials save only when submit succeeds |

**Do not call `POST /api/v1/auth/credentials` during signup / onboarding.**

That endpoint is **only** for existing users (migrate gate). See section 4.

---

## 1. Onboarding flow (new users)

```
1. register          → SMS OTP
2. verify-otp + imei → JWT (store Bearer)
3. KYC profile screens (name, ID, photos, …)
4. Username + PIN screen (UI only — NO API)
5. POST /api/v1/kyc/submit   ← username + pin + all KYC fields
6. KYC send-otp / confirm-otp (unchanged)
7. Wait for wallet / home
```

### Breaking change on submit

`POST /api/v1/kyc/submit`  
**Auth:** `Authorization: Bearer <token>`

**New required fields:**

| Field | Type | Rules |
|-------|------|--------|
| `username` | string | 3–20 chars; starts with letter; letters, digits, `.` `_` only |
| `pin` | string | Exactly **4 digits** |

These are the **app login** username/PIN — not `kraPin` (tax).

### Example body

```json
{
  "username": "jdoe",
  "pin": "1234",
  "firstName": "Jane",
  "middleName": "",
  "lastName": "Doe",
  "birthday": "1990-01-15",
  "gender": 1,
  "countryCode": "254",
  "mobile": "712345678",
  "idType": "101",
  "idNumber": "12345678",
  "frontSidePhoto": "<Base64 JPEG>",
  "selfiePhoto": "<Base64 JPEG>",
  "backSidePhoto": "<optional Base64>",
  "address": null,
  "kraPin": null,
  "email": null
}
```

### App behaviour rules

1. Collect username + PIN + confirm PIN on the Pin/Terms screen.
2. Keep them in memory / navigation args until submit — **no** `/auth/credentials` call.
3. On Submit, send them with the KYC payload.
4. If user goes **back** before submit → credentials are **not** saved → they can edit and submit again.
5. If submit fails (network / Choice) → retry with **same** username + PIN is OK.
6. Never log PIN. Never send PIN to any Choice Bank API yourselves (BFF/backend strips it).

---

## 2. Errors you must handle on submit

| `code` | HTTP | Meaning | UI |
|--------|------|---------|-----|
| `CREDENTIALS_REQUIRED` | 400 | Missing `username` or `pin` | Show validation |
| `USERNAME_INVALID` | 400 | Bad format / reserved name | Fix username |
| `USERNAME_TAKEN` | 409 | Someone else has this username | Ask for another |
| `INVALID_CREDENTIALS` | 400 | PIN not 4 digits | Fix PIN |
| `CREDENTIALS_ALREADY_SET` | 409 | User already has a **different** username | Rare after this fix; show support / login |

Same username on retry after a failed Choice call → **OK** (backend no-op).

---

## 3. Login (unchanged product rules)

`POST /api/v1/auth/login`

- Identifier: **username OR mobile** + **4-digit PIN** + **`imei`**
- Outcomes:
  - `AUTH_LOGIN_OK` → store JWT → home
  - `AUTH_DEVICE_OTP_REQUIRED` → device OTP → **back to login** (no JWT)
  - `AUTH_MUST_SET_CREDENTIALS` → migrate flow (section 4)

Details: [MOBILE_AUTH_HANDOFF.md](MOBILE_AUTH_HANDOFF.md) §C–F.

---

## 4. Existing users without username/PIN (migrate)

Old customers who registered before PIN login:

1. Login with **mobile** + any attempt → `mustSetCredentials: true` + OTP sent  
2. `POST /api/v1/auth/verify-migrate-otp`  
3. `POST /api/v1/auth/credentials` (Bearer) with `username`, `pin`, `imei`  
4. Back to login → normal PIN login  

**This is the only place** the app should call `/auth/credentials`.

Username-only login without credentials → `CREDENTIALS_NOT_SET_USE_MOBILE` — tell user to use phone number.

---

## 5. Checklist for mobile PR

- [ ] Remove signup call to `POST /auth/credentials`
- [ ] Pin/Terms screen only stores username + PIN locally
- [ ] `POST /kyc/submit` includes `username` + `pin`
- [ ] Map submit errors in section 2
- [ ] Back navigation before submit does not leave user stuck
- [ ] Migrate flow still uses `/auth/credentials` after migrate OTP
- [ ] Login uses username **or** mobile + PIN + `imei`
- [ ] Never log PIN / OTP

---

## 6. What did **not** change

- Signup OTP: `register` → `verify-otp` (+ `imei`)
- KYC OTP: `send-otp` / `confirm-otp` / status
- Wallet / transactions APIs
- Forgot PIN: `forgot-pin/request` + `confirm`
- Device re-bind OTP → return to login

---

## 7. Questions / contact

Backend contract source of truth:

- This doc (KYC submit credentials delta)
- [MOBILE_AUTH_HANDOFF.md](MOBILE_AUTH_HANDOFF.md) (full auth)
- [MOBILE_API_CONTRACT.md](MOBILE_API_CONTRACT.md)
