# Mobile Team — Add / Update Email

**Audience:** Android / iOS  
**Date:** 2026-08-31  
**Status:** Backend change — identity is **server-side**. App sends **email only**.  
**Base URL:** BFF only (never call wallet-service or Choice). Example: `http://app.vycepay.com:9090`

Related:

- [MOBILE_API_CONTRACT_DETAILED.md](MOBILE_API_CONTRACT_DETAILED.md) — envelopes
- [MOBILE_STATEMENT_EMAIL_CHANGE.md](MOBILE_STATEMENT_EMAIL_CHANGE.md) — statements use this email
- [MOBILE_AUTH_HANDOFF.md](MOBILE_AUTH_HANDOFF.md) — JWT

---

## What changed (for you)

| Area | Before | After |
|------|--------|--------|
| Add / update body | `onboardType`, `personalIdType`, `documentNumber`, `email` | **`{ "email": "..." }` only** |
| Verify current email | Identity JSON | **Empty body** (POST, no JSON required) |
| Verify email or mobile | Identity + `verifyType` | **`{ "verifyType": "email" }` only** |
| `accountId` / CNIC | Must not be collected for this flow | Backend loads ID from KYC |

Do **not** send `accountId`, `onboardType`, `personalIdType`, or `documentNumber`. Extra fields are ignored; identity is never taken from the app.

Mobile talks **only to BFF**. Do not call Choice paths.

---

## Product rules

| Rule | Detail |
|------|--------|
| Auth | `Authorization: Bearer <token>`. Do **not** send `X-Customer-Id`. |
| User input | **Email address** + OTP code only |
| Identity | JWT → KYC (`id_type`, `id_number`). `onboardType` is always `personal`. |
| OTP | `POST /api/v1/wallets/account/verify-otp` only — not auth / KYC / transfer OTP |
| `applicationId` | Save from each start-step `data.applicationId` for the next OTP |
| Messages | Show envelope `message` as-is. Log `requestId` for support. |
| After success | Refresh `GET /wallets/account/details` — `data.email` is now persisted on Choice success |

Wallet must exist (`GET /wallets/me` 200). If KYC has no ID on file → `KYC_IDENTITY_MISSING` (complete onboarding).

---

## Screen flows

### A) First-time add (`details.email` is null)

```
1. User enters new email
2. POST /api/v1/wallets/account/email     { "email": "..." }
   → save data.applicationId
   → OTP goes to the NEW email
3. POST /api/v1/wallets/account/verify-otp
   { "applicationId", "otpCode" }
4. POST /api/v1/wallets/account/verify-email-or-mobile
   { "verifyType": "email" }
   → if data.applicationId present, another OTP → verify-otp again
5. GET /api/v1/wallets/account/details  → data.email
```

If step 4 has no `applicationId`, skip the extra OTP.

### B) Change existing email

```
1. POST /api/v1/wallets/account/verify-email-address   (no body)
   → OTP to CURRENT email → verify-otp
2. User enters NEW email
3. POST /api/v1/wallets/account/email     { "email": "<new>" }
   → OTP to NEW email → verify-otp
4. POST /api/v1/wallets/account/verify-email-or-mobile
   { "verifyType": "email" }
   → OTP + verify-otp only if applicationId returned
5. Refresh account details
```

Step 1 is required by Choice: prove access to the current address before replacing it.

### C) Verify only (address already on file)

```
1. POST /api/v1/wallets/account/verify-email-address
2. verify-otp
3. POST .../verify-email-or-mobile  { "verifyType": "email" }
```

---

## Auth headers

| Header | When |
|--------|------|
| `Authorization: Bearer <token>` | All |
| `Content-Type: application/json` | POSTs that have a JSON body (`/email`, `/verify-email-or-mobile`, `/verify-otp`) |

`/verify-email-address` needs Bearer only (no body).

---

## APIs

Success envelope (typical):

```json
{
  "success": true,
  "code": "WALLET_EMAIL_UPDATED",
  "message": "Email update requested.",
  "requestId": "correlation-id",
  "data": {
    "applicationId": "...."
  }
}
```

Show `message` (Choice `msg` when present). Read `data.applicationId`.

### Prefill

`GET /api/v1/wallets/account/details` → `data.email` (`null` if none).  
Fallback: `GET /api/v1/auth/me` → `email` (profile object at root, not the success envelope).

### Add / replace

`POST /api/v1/wallets/account/email`

```json
{
  "email": "customer@example.com"
}
```

**Success `code`:** `WALLET_EMAIL_UPDATED`

Backend then calls Choice with KYC ID + this email, and saves `customer.email` on Choice success.

### Verify current email

`POST /api/v1/wallets/account/verify-email-address`

No body.

**Success `code`:** `WALLET_VERIFY_EMAIL_REQUESTED`

### Activate for future OTPs

`POST /api/v1/wallets/account/verify-email-or-mobile`

```json
{
  "verifyType": "email"
}
```

Use `"mobile"` only for the **phone-change** flow, not for email.

**Success `code`:** `WALLET_VERIFY_CONTACT_REQUESTED`

### Confirm OTP

`POST /api/v1/wallets/account/verify-otp`

```json
{
  "applicationId": "<from previous data.applicationId>",
  "otpCode": "123456"
}
```

**Success `code`:** `WALLET_ACCOUNT_OTP_VERIFIED`

Do **not** use `/auth/verify-otp`, `/kyc/confirm-otp`, or `/transactions/confirm-otp`.

### Resend

No wallet `resend-otp`. Call the **same start API** again and replace `applicationId` if a new one is returned.

---

## Errors

| `code` | HTTP | Mobile action |
|--------|------|----------------|
| `EMAIL_REQUIRED` / `INVALID_EMAIL` | 400 | Fix the email field |
| `KYC_IDENTITY_MISSING` | 409 | KYC / ID not on file — send to onboarding, do **not** ask for CNIC here |
| `INVALID_VERIFY_TYPE` | 400 | Send `verifyType` `email` or `mobile` |
| `WALLET_NOT_FOUND` | 404 | Complete KYC / wait for wallet |
| `SERVICE_UNAVAILABLE` | 503 | Retry later |
| `CHOICE_BANK_ERROR` / `BAD_GATEWAY` | 502 | Show `message` |
| `UNAUTHORIZED` | 401 | Re-login |

Always show envelope `message`.

---

## Suggested copy

| State | Copy |
|-------|------|
| No email | “Add an email to receive statements and verification codes.” |
| Has email | Show address + “Change email” |
| OTP (current) | “Enter the code sent to your current email.” |
| OTP (new) | “Enter the code sent to {newEmail}.” |
| Success | “Email verified.” |
| Error | Server `message` |

---

## QA checklist

- [ ] Body of `/email` is **only** `{ "email": "..." }` — no ID / accountId  
- [ ] `/verify-email-address` has **no** body  
- [ ] `/verify-email-or-mobile` sends only `verifyType`  
- [ ] OTP uses `/wallets/account/verify-otp`  
- [ ] After add, `GET .../details` shows the new `email`  
- [ ] User is **never** asked for CNIC / account number on this screen  
- [ ] Envelope `message` shown as-is  

---

## Curl

```bash
# Add / update
curl -s -X POST "$BFF/api/v1/wallets/account/email" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com"}'

# Verify current (no body)
curl -s -X POST "$BFF/api/v1/wallets/account/verify-email-address" \
  -H "Authorization: Bearer $TOKEN"

# Activate email for OTPs
curl -s -X POST "$BFF/api/v1/wallets/account/verify-email-or-mobile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"verifyType":"email"}'

# OTP
curl -s -X POST "$BFF/api/v1/wallets/account/verify-otp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"applicationId":"<applicationId>","otpCode":"123456"}'
```

---

## Out of scope

- Collecting ID / accountId in the app for this flow  
- Name / CNIC / address updates (no Choice API after account open)  
- Phone change (separate: `/mobile-change` — BFF still injects `accountId`)  
- `account/cancelAccountOperation` (not on BFF)

---

## Quick reference

| Action | Method | Path | Body |
|--------|--------|------|------|
| Prefill | `GET` | `/api/v1/wallets/account/details` | — |
| Add / replace | `POST` | `/api/v1/wallets/account/email` | `{ "email" }` |
| Verify current | `POST` | `/api/v1/wallets/account/verify-email-address` | — |
| Activate contact | `POST` | `/api/v1/wallets/account/verify-email-or-mobile` | `{ "verifyType": "email" }` |
| OTP | `POST` | `/api/v1/wallets/account/verify-otp` | `applicationId`, `otpCode` |

Success codes: `WALLET_EMAIL_UPDATED` · `WALLET_VERIFY_EMAIL_REQUESTED` · `WALLET_VERIFY_CONTACT_REQUESTED` · `WALLET_ACCOUNT_OTP_VERIFIED`
