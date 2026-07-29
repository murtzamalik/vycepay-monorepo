# VycePay Mobile — Validate Account (Hakikisha) Handoff

**Audience:** Mobile team  
**Purpose:** Add account title-fetch before send money. Backend is live; this doc is the only mobile deliverable from backend.  
**Base URL:** BFF only (never call transaction-service ports directly). Example: `https://<bff-host>`

> Auth, KYC, wallet, OTP, and deposit flows are unchanged. This document covers **validate account + send body change** only.

---

## 1. Product rules (must follow)

| Rule | Detail |
|------|--------|
| Order | Bank select → account number → **validate / title fetch** → show name → user confirms → amount → send → OTP (if required) |
| Title source | Always from `POST /validate-account` (Choice Bank). Never let the user type the beneficiary name as source of truth. |
| Send re-check | Backend **re-validates** on every `POST /send` and overwrites payee name. Client-typed `payeeAccountName` is ignored. |
| Frozen / restrict | If validate returns error `ACCOUNT_FROZEN` or `ACCOUNT_RESTRICT_IN`, block Continue / Send. |
| PesaLink | When paying another bank (`accountType = 4`), `bankCode` / `payeeBankCode` is mandatory (from bank-codes list). |

---

## 2. Auth headers

| Header | When |
|--------|------|
| `Content-Type: application/json` | JSON bodies |
| `Authorization: Bearer <token>` | All transaction APIs |
| `Idempotency-Key` | **Required** on `POST /send` only (unique per send attempt) |

Do **not** send `X-Customer-Id` from the app — BFF sets it from JWT.

---

## 3. `accountType` mapping

Use the same integer Choice Bank expects:

| accountType | Rail | `bankCode` / `payeeBankCode` |
|-------------|------|------------------------------|
| `0` | Choice Bank (internal) | Not required for validate |
| `1` | M-Pesa Pay Bill | Not required |
| `2` | M-Pesa Pay Till | Not required |
| `3` | M-Pesa Mobile | Not required; `payeeBankCode` still sent on send as your M-Pesa bank code (e.g. from bank-codes) |
| `4` | PesaLink (other banks) | **Required** — use code from `GET /bank-codes` |
| `5` | IMT Validate | Per Choice; confirm with product if used |

On **send** for PesaLink (`4`), set `payeeBankCode` to the same bank code used as `bankCode` in validate.

---

## 4. Screen flow

1. Load banks: `GET /api/v1/transactions/bank-codes`
2. User picks bank / rail → map to `accountType` (+ `bankCode` if 4)
3. User enters account / mobile / shortcode
4. Call validate (debounce on “Continue” or after account field blur — prefer explicit Continue)
5. Show confirmation: **Paying: {accountName}**
6. If `valid != true` or HTTP error → show message; do not go to amount
7. Amount + optional remark → `POST /send` with **same** `accountType` and account/bank values
8. Existing OTP / status / history flow

---

## 5. Validate account API

### Request

- **POST** `/api/v1/transactions/validate-account`
- **Auth:** Bearer required

```json
{
  "accountId": "0123456789",
  "accountType": 4,
  "bankCode": "01"
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `accountId` | Yes | Account number, MSISDN, paybill, or till |
| `accountType` | Yes | Integer 0–5 |
| `bankCode` | When type is 4 | From bank-codes; omit otherwise |

### Success response (`200`)

```json
{
  "accountId": "0123456789",
  "accountType": 4,
  "accountName": "JOHN DOE",
  "freezeStatus": 0,
  "restrictStatus": 0,
  "valid": true
}
```

| Field | Meaning |
|-------|---------|
| `accountName` | Show on confirm screen |
| `freezeStatus` | `0` normal, `1` frozen (backend also errors) |
| `restrictStatus` | `0` normal, `1` restrict in (cannot receive), `2` restrict out |
| `valid` | `true` only when receivable |

### Error codes (handle in UI)

| Code | HTTP | User-facing suggestion |
|------|------|------------------------|
| `INVALID_ACCOUNT_ID` | 400 | Enter a valid account number |
| `INVALID_ACCOUNT_TYPE` | 400 | App bug — check mapping |
| `BANK_CODE_REQUIRED` | 400 | Select a bank (PesaLink) |
| `ACCOUNT_FROZEN` | 409 | This account is frozen and cannot receive money |
| `ACCOUNT_RESTRICT_IN` | 409 | This account cannot receive money |
| `CHOICE_*` / Choice upstream | varies | Show server `message`; allow retry |
| `CUSTOMER_NOT_FOUND` | 404 | Session / account issue — re-login |

---

## 6. Send money API (breaking change)

### Request

- **POST** `/api/v1/transactions/send`
- **Headers:** `Authorization`, `Idempotency-Key` (required)

```json
{
  "payeeBankCode": "01",
  "payeeAccountId": "0123456789",
  "accountType": 4,
  "amount": 100.50,
  "remark": "Optional note"
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `payeeBankCode` | Yes | Bank / rail code; for type `4` must match validate `bankCode` |
| `payeeAccountId` | Yes | Same as validate `accountId` |
| `accountType` | **Yes (new)** | Same as validate |
| `amount` | Yes | KES |
| `remark` | No | Max 100 chars server-side |
| `payeeAccountName` | No | Optional; **server overwrites** from Choice |

### Response

Unchanged: `TransactionResponse` with `externalId` used as `transactionId` for OTP / status.

OTP (unchanged):

- `POST /api/v1/transactions/send-otp?transactionId=<externalId>&otpType=SMS`
- `POST /api/v1/transactions/confirm-otp?transactionId=<externalId>&otpCode=<code>`

---

## 7. Examples

### PesaLink (other bank)

```http
POST /api/v1/transactions/validate-account
Authorization: Bearer <token>
Content-Type: application/json

{"accountId":"0123456789","accountType":4,"bankCode":"01"}
```

```http
POST /api/v1/transactions/send
Authorization: Bearer <token>
Idempotency-Key: <uuid>
Content-Type: application/json

{"payeeBankCode":"01","payeeAccountId":"0123456789","accountType":4,"amount":500}
```

### M-Pesa mobile

```http
POST /api/v1/transactions/validate-account
Authorization: Bearer <token>
Content-Type: application/json

{"accountId":"254712345678","accountType":3}
```

```http
POST /api/v1/transactions/send
Authorization: Bearer <token>
Idempotency-Key: <uuid>
Content-Type: application/json

{"payeeBankCode":"<mpesa-code-from-bank-codes>","payeeAccountId":"254712345678","accountType":3,"amount":100}
```

---

## 8. QA checklist

- [ ] Bank list loads from `/bank-codes`
- [ ] PesaLink: validate without `bankCode` → `BANK_CODE_REQUIRED`
- [ ] Valid PesaLink account → UI shows Choice `accountName`
- [ ] User can change account and re-validate
- [ ] Frozen / restrict-in → blocked with clear message
- [ ] Send includes `accountType`; succeeds after validate
- [ ] Send response `payeeAccountName` matches validated title (not a typed name)
- [ ] OTP / status / history still work after send
- [ ] Missing Bearer → 401; do not send `X-Customer-Id`

---

## 9. Related docs

- [MOBILE_API_CONTRACT.md](MOBILE_API_CONTRACT.md)
- [MOBILE_API_CONTRACT_DETAILED.md](MOBILE_API_CONTRACT_DETAILED.md) — sections 15–16
- [BUSINESS_LOGIC_AND_FLOWS.md](BUSINESS_LOGIC_AND_FLOWS.md) — Send Money
- Choice Bank: [Hakikisha Validate Account](https://choice-bank.gitbook.io/choice-bank/transfer/new-features#hakikisha-validate-account)
