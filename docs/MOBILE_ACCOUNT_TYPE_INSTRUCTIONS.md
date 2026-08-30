# Mobile Team — `accountType` Instruction (Send Money)

**Audience:** Android / iOS  
**Why:** `POST /transactions/send` aur `POST /transactions/validate-account` dono pe **`accountType` required** hai. Missing hone pe backend return karta hai:

```text
INVALID_ACCOUNT_TYPE — accountType must be an integer between 0 and 5
```

**Base URL:** BFF only — `http://app.vycepay.com:9090` (ya prod BFF)

---

## 1. Quick rule

| User UI action | `accountType` (integer) | `bankCode` / `payeeBankCode` |
|----------------|-------------------------|------------------------------|
| Choice Bank account (internal) | `0` | Optional / as product says |
| M-Pesa **Paybill** | `1` | Not required on validate |
| M-Pesa **Till** | `2` | Not required on validate |
| M-Pesa **mobile number** | `3` | On **send**, still send M-Pesa rail code from bank-codes (e.g. `M-PESA`) as `payeeBankCode` |
| **Other bank** (PesaLink) | `4` | **Required** — PesaLink bank code from list (e.g. `"01"`, `"68"`). **Not** `"46"` (Choice) — see [MOBILE_CHOICE_VS_PESALINK.md](MOBILE_CHOICE_VS_PESALINK.md) |
| IMT | `5` | Per product / Choice |

**Important**

- `accountType` = **0–5 number**, bank list ka code nahi.
- `bankCode` / `payeeBankCode` = `"46"`, `"01"`, `"M-PESA"` wagaira — yeh `accountType` **nahi** hai.
- Type **integer** bhejo (`4`), string `"4"` avoid karo agar JSON mapper issue ho.

---

## 2. APIs where `accountType` is required

### A) Validate (title fetch) — pehle yeh

`POST /api/v1/transactions/validate-account`

```json
{
  "accountId": "<account / mobile / paybill / till>",
  "accountType": 4,
  "bankCode": "68"
}
```

> Choice / Vyce accounts use `accountType: 0` **without** treating `"46"` as PesaLink. Full detail: [MOBILE_CHOICE_VS_PESALINK.md](MOBILE_CHOICE_VS_PESALINK.md)

| Field | When |
|-------|------|
| `accountId` | Always |
| `accountType` | Always (0–5) |
| `bankCode` | **Only when `accountType = 4`** |

### B) Send money — phir yeh

`POST /api/v1/transactions/send`  
Headers: `Authorization`, `Idempotency-Key`

```json
{
  "payeeAccountId": "<same as validate accountId>",
  "payeeBankCode": "46",
  "accountType": 4,
  "amount": 10.0,
  "remark": "Bank transfer"
}
```

| Field | Required |
|-------|----------|
| `payeeAccountId` | Yes |
| `payeeBankCode` | Yes |
| **`accountType`** | **Yes — ab mandatory** |
| `amount` | Yes |
| `remark` | No |
| `payeeAccountName` | No (server Choice se overwrite karta hai) |

Validate aur send pe **same** `accountType` + same account/bank values use karo.

---

## 3. Per-transaction examples

### 3.1 Other bank / PesaLink (`accountType = 4`)

Banks list: `GET /api/v1/transactions/bank-codes` → pick an **external** bank code (e.g. `"68"` Equity).  
**Do not** use `"46"` (Choice Microfinance) with type `4` — that is Choice internal → use type `0` (see [MOBILE_CHOICE_VS_PESALINK.md](MOBILE_CHOICE_VS_PESALINK.md)).

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
  "amount": 10.0,
  "remark": "Bank transfer"
}
```

### 3.2 M-Pesa mobile (`accountType = 3`)

**Validate**
```json
{
  "accountId": "712345678",
  "accountType": 3
}
```

**Send**
```json
{
  "payeeAccountId": "712345678",
  "payeeBankCode": "M-PESA",
  "accountType": 3,
  "amount": 100.0
}
```

### 3.3 M-Pesa Paybill (`accountType = 1`)

**Validate**
```json
{
  "accountId": "123456",
  "accountType": 1
}
```

**Send**
```json
{
  "payeeAccountId": "123456",
  "payeeBankCode": "M-PESA",
  "accountType": 1,
  "amount": 50.0,
  "payeeReferenceNumber": "001122"
}
```

### 3.4 M-Pesa Till (`accountType = 2`)

**Validate**
```json
{
  "accountId": "987654",
  "accountType": 2
}
```

**Send**
```json
{
  "payeeAccountId": "987654",
  "payeeBankCode": "M-PESA",
  "accountType": 2,
  "amount": 50.0
}
```

> Paybill / Till send pe backend Choice `trans/v2/applyForMpesaBusinessTransfer` call karta hai. Paybill pe `payeeReferenceNumber` **required** hai (`INVALID_PAYEE_REFERENCE` agar missing).

### 3.5 Choice internal (`accountType = 0`)

**Validate**
```json
{
  "accountId": "46012001327585",
  "accountType": 0
}
```

**Send**
```json
{
  "payeeAccountId": "46012001327585",
  "payeeBankCode": "46",
  "accountType": 0,
  "amount": 100.0
}
```

> This is the correct mapping for Choice Microfinance accounts (bank list code `"46"`). Never use `accountType: 4` for these.

---

## 4. Bug we saw in production (fix this)

**Request that failed (400) — missing accountType (older):**

```json
{
  "amount": 10.0,
  "payeeAccountId": "46012001327585",
  "payeeAccountName": "Masaki",
  "payeeBankCode": "46",
  "remark": "Bank transfer"
}
```

**Fix:** add `"accountType"` — for this Choice account use **`0`**, not `4`.

**Second bug (400 from Choice):** `accountType: 4` + `bankCode: "46"` → `CHOICE_BANK_CODE_NOT_FOUND` / `14012`.  
See [MOBILE_CHOICE_VS_PESALINK.md](MOBILE_CHOICE_VS_PESALINK.md).

---

## 5. Suggested app mapping

| Screen / rail selected | Set `accountType` |
|------------------------|-------------------|
| “Send to bank” / bank picker (external) | `4` + selected bank’s `bankCode` (not Choice `"46"`) |
| “Choice / Vyce account” | `0` |
| “Send to M-Pesa number” | `3` |
| “Paybill” | `1` |
| “Till / Buy goods” | `2` |

Bank picker se jo code aaye (`"01"`, `"46"`, …) → sirf `bankCode` / `payeeBankCode` mein daalo — **`accountType` mein mat daalo**.

---

## 6. Checklist before release

- [ ] Validate body always includes `accountType` (0–5)
- [ ] Send body always includes `accountType` (same as validate)
- [ ] PesaLink (`4`) always sends `bankCode` / `payeeBankCode`
- [ ] Handle `INVALID_ACCOUNT_TYPE`, `BANK_CODE_REQUIRED`, `ACCOUNT_FROZEN`, `ACCOUNT_RESTRICT_IN`
- [ ] Do not rely on user-typed name; show `accountName` from validate response

---

## 7. More detail

Full flow: [MOBILE_VALIDATE_ACCOUNT_HANDOFF.md](MOBILE_VALIDATE_ACCOUNT_HANDOFF.md)
