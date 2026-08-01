# Mobile Team — Beneficiaries Handoff

**Audience:** Android / iOS  
**Purpose:** Save payees after transfer and pay again from a list. Backend is live on BFF.  
**Base URL:** BFF only (never call transaction-service ports directly). Example: `http://app.vycepay.com:9090`

Related:

- [MOBILE_VALIDATE_ACCOUNT_HANDOFF.md](MOBILE_VALIDATE_ACCOUNT_HANDOFF.md) — validate + send
- [MOBILE_ACCOUNT_TYPE_INSTRUCTIONS.md](MOBILE_ACCOUNT_TYPE_INSTRUCTIONS.md) — `accountType` 0–5
- [MOBILE_CHOICE_VS_PESALINK.md](MOBILE_CHOICE_VS_PESALINK.md) — Choice (`0`) vs PesaLink (`4`)

---

## 1. Product rules (must follow)

| Rule | Detail |
|------|--------|
| Save | **Opt-in** after successful transfer (or after OTP confirm). Do not auto-save silently. |
| List | Show saved beneficiaries on Send Money (tab or section). |
| Select | Pre-fill rail + account; **always** call `POST /validate-account` again before send. |
| Never skip Hakikisha | Saved ≠ trusted forever. Account may be frozen / restrict-in later. |
| Edit | Nickname only. |
| Delete | Soft-delete on backend; remove from UI list. |
| IDs | Use `externalId` (UUID) in APIs — never invent sequential ids. |

---

## 2. Screen flows

### A) New pay → optional save

```
1. Enter rail + account (or bank picker)
2. POST /validate-account → show accountName
3. Amount → POST /send (+ OTP if required)
4. Success screen → “Save as beneficiary?”
5. If Yes → nickname → POST /beneficiaries
6. Done
```

### B) Pay from beneficiaries

```
1. Open Beneficiaries list → GET /beneficiaries
2. Tap item
3. POST /validate-account with stored accountType + accountId + bankCode (if type 4)
4. Show confirmation with fresh accountName
5. Amount → POST /send (+ OTP if required)
6. Optional: “Update saved name?” not required (upsert on next save)
```

### C) Manage

```
- Rename → PATCH /beneficiaries/{externalId} { nickname }
- Remove → DELETE /beneficiaries/{externalId}
```

---

## 3. Auth headers

| Header | When |
|--------|------|
| `Content-Type: application/json` | JSON bodies |
| `Authorization: Bearer <token>` | All beneficiary APIs |
| `Idempotency-Key` | **Only** on `POST /send` (not on beneficiary CRUD) |

Do **not** send `X-Customer-Id` — BFF sets it from JWT.

---

## 4. APIs

Base path: `/api/v1/transactions/beneficiaries`

### 4.1 List

`GET /api/v1/transactions/beneficiaries`

**Response `200`:**

```json
{
  "items": [
    {
      "externalId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "nickname": "Mum Equity",
      "accountType": 4,
      "payeeBankCode": "68",
      "payeeAccountId": "0123456789",
      "payeeAccountName": "JANE DOE"
    }
  ]
}
```

Empty list → `{ "items": [] }`.

**UI tips**

- Title: `nickname` (fallback `payeeAccountName`)
- Subtitle: mask account (`****6789`) + bank/rail label from `accountType` / `payeeBankCode`

---

### 4.2 Create / upsert (save)

`POST /api/v1/transactions/beneficiaries`

```json
{
  "nickname": "Mum Equity",
  "accountType": 4,
  "payeeBankCode": "68",
  "payeeAccountId": "0123456789",
  "payeeAccountName": "JANE DOE"
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `nickname` | Yes | 1–50 chars |
| `accountType` | Yes | Integer 0–5 |
| `payeeAccountId` | Yes | Same as validate `accountId` |
| `payeeBankCode` | If type `4` | Same as validate `bankCode`. For other rails may be `""` or omit (stored as empty). |
| `payeeAccountName` | Recommended | From last successful validate / send response |

**Success**

- New: HTTP `201`, code `BENEFICIARY_SAVED`
- Same identity already saved (or soft-deleted restored): HTTP `200`, code `BENEFICIARY_UPDATED`

```json
{
  "success": true,
  "code": "BENEFICIARY_SAVED",
  "message": "Beneficiary saved.",
  "requestId": "...",
  "data": {
    "externalId": "...",
    "nickname": "Mum Equity",
    "accountType": 4,
    "payeeBankCode": "68",
    "payeeAccountId": "0123456789",
    "payeeAccountName": "JANE DOE"
  }
}
```

**When to call**

- After transfer success, user confirms save (use payee fields from send/validate response).
- Or manual “Add beneficiary” after a successful validate (same body).

---

### 4.3 Rename

`PATCH /api/v1/transactions/beneficiaries/{externalId}`

```json
{
  "nickname": "Mum"
}
```

Success code: `BENEFICIARY_UPDATED`.

---

### 4.4 Delete

`DELETE /api/v1/transactions/beneficiaries/{externalId}`

Success code: `BENEFICIARY_DELETED`.  
Item must disappear from next `GET`.

---

## 5. Mapping select → validate + send

From list item:

| Beneficiary field | Validate body | Send body |
|-------------------|---------------|-----------|
| `payeeAccountId` | `accountId` | `payeeAccountId` |
| `accountType` | `accountType` | `accountType` |
| `payeeBankCode` | `bankCode` (only if type `4`) | `payeeBankCode` |
| — | — | `amount`, `remark`, `Idempotency-Key` |

### Example — PesaLink beneficiary

**Validate**

```json
{
  "accountId": "0123456789",
  "accountType": 4,
  "bankCode": "68"
}
```

**Send**

```json
{
  "payeeAccountId": "0123456789",
  "payeeBankCode": "68",
  "accountType": 4,
  "amount": 500.0,
  "remark": "Rent"
}
```

### Example — Choice internal beneficiary

```json
{ "accountId": "46012001327585", "accountType": 0 }
```

```json
{
  "payeeAccountId": "46012001327585",
  "payeeBankCode": "46",
  "accountType": 0,
  "amount": 100.0
}
```

> Never save/send Choice accounts as `accountType: 4` + `bankCode: "46"`. See Choice vs PesaLink guide.

### Example — M-Pesa mobile

```json
{ "accountId": "712345678", "accountType": 3 }
```

```json
{
  "payeeAccountId": "712345678",
  "payeeBankCode": "M-PESA",
  "accountType": 3,
  "amount": 50.0
}
```

---

## 6. Error codes

| Code | HTTP | UI |
|------|------|-----|
| `INVALID_NICKNAME` | 400 | Fix nickname |
| `INVALID_ACCOUNT_TYPE` | 400 | App bug — check mapping |
| `INVALID_ACCOUNT_ID` | 400 | Missing account |
| `BANK_CODE_REQUIRED` | 400 | Type 4 needs bank code |
| `BENEFICIARY_NOT_FOUND` | 404 | Refresh list |
| `CUSTOMER_NOT_FOUND` | 404 | Re-login |
| `CHOICE_BANK_CODE_NOT_FOUND` / `14012` | 400 | Wrong rail (often Choice as PesaLink) |
| `ACCOUNT_FROZEN` / `ACCOUNT_RESTRICT_IN` | 409 | Block pay; show message |

Validate/send errors are unchanged — handle them even when paying from a saved beneficiary.

---

## 7. Suggested UI copy

| Moment | Copy |
|--------|------|
| After success | “Save this payee for next time?” |
| Nickname field | “Name (e.g. Mum Equity)” |
| Empty list | “No saved beneficiaries yet. They appear after you save a payee.” |
| Delete confirm | “Remove this beneficiary?” |

---

## 8. Mobile checklist

- [ ] Beneficiaries tab/section on Send Money
- [ ] `GET` list on open / pull-to-refresh
- [ ] Tap → validate → confirm name → amount → send
- [ ] Success → opt-in save → `POST` with nickname + rail fields
- [ ] Handle `BENEFICIARY_SAVED` vs `BENEFICIARY_UPDATED` (idempotent)
- [ ] Rename + delete wired
- [ ] Mask account numbers in list
- [ ] Choice vs PesaLink rules still applied when saving/paying
- [ ] Never skip validate for saved payees
- [ ] Secure token storage unchanged

---

## 9. What did **not** change

- `POST /validate-account` and `POST /send` contracts (no `beneficiaryId` on send)
- Deposit, utilities, KYC, auth
- Still call **BFF only**
