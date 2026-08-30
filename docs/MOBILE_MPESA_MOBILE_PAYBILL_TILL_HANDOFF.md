# Mobile Team — M-Pesa Send to Mobile / Paybill / Buy Goods (Till)

**Audience:** Android / iOS  
**Purpose:** Teen screens banani hain aur BFF pe integrate karni hain — Send Money to Mobile, Paybill, Buy Goods (Till).  
**Base URL:** BFF only (transaction-service ports mat call karo). Example: `http://app.vycepay.com:9090` ya prod BFF.

Related:

- [MOBILE_MPESA_PAYBILL_TILL_B2B_CHANGE.md](MOBILE_MPESA_PAYBILL_TILL_B2B_CHANGE.md) — **what changed for mobile** (Paybill `payeeReferenceNumber`)
- [MOBILE_VALIDATE_ACCOUNT_HANDOFF.md](MOBILE_VALIDATE_ACCOUNT_HANDOFF.md) — validate + send rules
- [MOBILE_ACCOUNT_TYPE_INSTRUCTIONS.md](MOBILE_ACCOUNT_TYPE_INSTRUCTIONS.md) — `accountType` 0–5
- [MOBILE_BENEFICIARIES_HANDOFF.md](MOBILE_BENEFICIARIES_HANDOFF.md) — save / search beneficiaries
- [MOBILE_API_CONTRACT.md](MOBILE_API_CONTRACT.md) — full transaction contract

---

## 1. Quick mapping (product → API)

| Screen (I&M-style) | `accountType` | `payeeAccountId` / `accountId` | `payeeBankCode` on send |
|--------------------|---------------|--------------------------------|-------------------------|
| **Send Money to Mobile** | `3` | MSISDN (e.g. `712345678` or `254712345678`) | `"M-PESA"` (from bank-codes) |
| **M-PESA Paybill** | `1` | Business / Paybill number | `"M-PESA"` |
| **M-PESA Buy Goods (Till)** | `2` | Till number | `"M-PESA"` |

**Important**

- `accountType` = integer `1` / `2` / `3` — string `"1"` avoid karo.
- Ye teen rails **PesaLink nahi** hain — `accountType: 4` mat use karo.
- Validate aur send pe **same** `accountType` + same account value.

---

## 2. Product rules (must follow)

| Rule | Detail |
|------|--------|
| Order | Enter number → **validate** → show name → amount → send → OTP (if Choice requires) |
| Title | Sirf `POST /validate-account` se `accountName`. User-typed name source of truth nahi. |
| Re-validate | Backend har `POST /send` pe dubara validate karta hai; client `payeeAccountName` ignore hota hai. |
| Frozen / restrict | `ACCOUNT_FROZEN` / `ACCOUNT_RESTRICT_IN` → Continue / Send block. |
| Idempotency | Har send attempt pe **naya** `Idempotency-Key` (UUID). |
| Balance | Amount screen pe wallet balance dikhao (`GET` wallet). Amount ≤ available. |
| Remark | Optional payment description → `remark` field. |
| Schedule payment | **Supported nahi** — UI mein mat dikhao. |
| Paybill Account Number | **Required** on send as `payeeReferenceNumber` (Choice Paybill account / Acc number). Do **not** put it only in `remark`. |

**Backend Choice rails (mobile does not call Choice directly)**

| `accountType` | Choice path |
|---------------|-------------|
| `3` (mobile) | `trans/v2/applyForTransfer` |
| `1` (Paybill) / `2` (Till) | `trans/v2/applyForMpesaBusinessTransfer` |

---

## 3. Auth headers

| Header | When |
|--------|------|
| `Content-Type: application/json` | JSON bodies |
| `Authorization: Bearer <token>` | All APIs |
| `Idempotency-Key` | **Required** on `POST /send` only |

`X-Customer-Id` mat bhejo — BFF JWT se set karta hai.

---

## 4. Shared screen flow (all 3)

```
1. User opens screen (Mobile / Paybill / Till)
2. Optional: Search beneficiaries / contacts (pre-fill number)
3. User enters payee identifier
4. Tap Continue → POST /api/v1/transactions/validate-account
5. Show “Paying: {accountName}” (agar valid)
6. Enter amount (KES) + optional remark
7. Next → POST /api/v1/transactions/send  (+ Idempotency-Key)
8. If OTP required:
     POST /send-otp?transactionId=<externalId>&otpType=SMS
     POST /confirm-otp?transactionId=<externalId>&otpCode=<code>
9. Success → optional “Save as beneficiary?”
10. Status / history via existing transaction APIs
```

`payeeBankCode` for these three: use M-Pesa code from `GET /api/v1/transactions/bank-codes` (typically `"M-PESA"`). Cache once per session.

---

## 5. APIs used

### 5.1 Validate (Hakikisha)

`POST /api/v1/transactions/validate-account`

| Field | Required | Notes |
|-------|----------|--------|
| `accountId` | Yes | MSISDN / paybill / till |
| `accountType` | Yes | `3` / `1` / `2` |
| `bankCode` | No | Omit for types 1, 2, 3 |

Success (`200`): `accountName`, `valid: true`, freeze/restrict fields.  
Error: show envelope `message`; do not proceed to amount.

### 5.2 Send

`POST /api/v1/transactions/send`  
Headers: `Authorization`, `Idempotency-Key`

| Field | Required | Notes |
|-------|----------|--------|
| `payeeAccountId` | Yes | Same as validate `accountId` |
| `payeeBankCode` | Yes | `"M-PESA"` |
| `accountType` | Yes | Same as validate |
| `amount` | Yes | KES, `BigDecimal`-safe (e.g. `10.0`) |
| `payeeReferenceNumber` | **Yes when `accountType` is `1`** | Paybill account / Acc number → Choice `payeeReferenNumber`. Omit for Till (`2`). |
| `remark` | No | Optional description (not a substitute for Paybill reference) |
| `payeeAccountName` | No | Ignored / overwritten by server |

Response: `TransactionResponse` with `externalId` (UUID) — OTP / status ke liye yahi use karo.

### 5.3 OTP (jab Choice require kare)

- `POST /api/v1/transactions/send-otp?transactionId=<externalId>&otpType=SMS`
- `POST /api/v1/transactions/resend-otp?transactionId=<externalId>&otpType=SMS`
- `POST /api/v1/transactions/confirm-otp?transactionId=<externalId>&otpCode=<code>`

Success confirm code: `TXN_OTP_CONFIRMED`.

---

## 6. Screen A — Send Money to Mobile (`accountType = 3`)

### UI fields

| UI | Maps to |
|----|---------|
| Phone number | `accountId` / `payeeAccountId` |
| Amount (KES) | `amount` |
| Payment description (optional) | `remark` |
| My Number / Other Number | Client-only; Other → editable field |
| Contacts / beneficiaries | Pre-fill number only |

### Validate

```json
{
  "accountId": "712345678",
  "accountType": 3
}
```

### Send

```json
{
  "payeeAccountId": "712345678",
  "payeeBankCode": "M-PESA",
  "accountType": 3,
  "amount": 10.0,
  "remark": "Send to mobile"
}
```

### Mobile checklist

- [ ] Normalize MSISDN (leading `0` / `+254` / `254` — product rule; same value validate + send)
- [ ] Validate before amount screen
- [ ] Show `accountName` confirmation
- [ ] `Idempotency-Key` per attempt
- [ ] OTP flow if required
- [ ] Optional save beneficiary after success

---

## 7. Screen B — Paybill (`accountType = 1`)

### UI fields

| UI | Maps to |
|----|---------|
| Paybill / Business number | `accountId` / `payeeAccountId` |
| Account Number (reference) | **`payeeReferenceNumber`** (required on send) |
| Amount (KES) | `amount` |
| Description (optional) | `remark` |

### Validate

```json
{
  "accountId": "247247",
  "accountType": 1
}
```

### Send

```json
{
  "payeeAccountId": "247247",
  "payeeBankCode": "M-PESA",
  "accountType": 1,
  "amount": 10.0,
  "payeeReferenceNumber": "0820176326076",
  "remark": "optional note"
}
```

### Mobile checklist

- [ ] Paybill number required, numeric
- [ ] Account Number field → `payeeReferenceNumber` (required; missing → `INVALID_PAYEE_REFERENCE`)
- [ ] Same validate → send flow as Screen A
- [ ] Beneficiaries optional

---

## 8. Screen C — Buy Goods / Till (`accountType = 2`)

### UI fields

| UI | Maps to |
|----|---------|
| Till number | `accountId` / `payeeAccountId` |
| Amount (KES) | `amount` |
| Description (optional) | `remark` |

### Validate

```json
{
  "accountId": "987654",
  "accountType": 2
}
```

### Send

```json
{
  "payeeAccountId": "987654",
  "payeeBankCode": "M-PESA",
  "accountType": 2,
  "amount": 50.0,
  "remark": "Buy goods"
}
```

### Mobile checklist

- [ ] Till number required
- [ ] Do **not** require `payeeReferenceNumber`
- [ ] Validate → confirm name → amount → send
- [ ] OTP if required
- [ ] Beneficiaries optional

---

## 9. Payments hub entry points (suggested)

| Hub tile | Navigate to | `accountType` |
|----------|-------------|---------------|
| Send Money to Mobile | Screen A | `3` |
| M-PESA Paybill | Screen B | `1` |
| M-PESA Buy Goods | Screen C | `2` |

Debit account: logged-in user’s wallet (server `payerAccountId` set karta hai — client debit account picker optional / display-only).

---

## 10. Common errors (show envelope `message`)

| Situation | Typical client action |
|-----------|------------------------|
| Missing / invalid `accountType` | Fix mapping; must be int 1/2/3 |
| Validate fail / frozen | Block send; show message |
| Wrong OTP | Allow resend / retry |
| Duplicate send | New `Idempotency-Key` for new attempt; same key → same result |

Never invent mobile-side remapping of Choice / API `message`.

---

## 11. Out of scope (this handoff)

- Schedule / recurring payment
- Airtime, utilities, M-Pesa deposit (STK) — alag APIs
- PesaLink / other banks (`accountType: 4`)
- Choice internal transfer (`accountType: 0`)

---

## 12. QA smoke (sandbox / staging)

1. **Mobile:** validate `accountType: 3` → send KES 10 → OTP if prompted → success / pending  
2. **Paybill:** validate `1` → send → confirm  
3. **Till:** validate `2` → send → confirm  
4. Wrong number → validate error, no send  
5. Retry send with **new** Idempotency-Key  
6. Beneficiary save + pay-again re-validates before send  
