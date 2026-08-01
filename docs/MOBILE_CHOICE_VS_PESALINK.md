# Mobile Team — Choice vs PesaLink (`bankCode` + `accountType`)

**Audience:** Android / iOS  
**Date:** 2026-07-30  
**Why this doc:** Validate call fail ho rahi thi with Choice error `14012 — bank code does not exist` jab app ne Choice account ko PesaLink ki tarah bheja.

Related: [MOBILE_ACCOUNT_TYPE_INSTRUCTIONS.md](MOBILE_ACCOUNT_TYPE_INSTRUCTIONS.md) · [MOBILE_VALIDATE_ACCOUNT_HANDOFF.md](MOBILE_VALIDATE_ACCOUNT_HANDOFF.md)

---

## 1. Root cause (production bug)

### Request that failed

```json
{
  "accountId": "46012001327585",
  "accountType": 4,
  "bankCode": "46"
}
```

### Choice response

```text
code=14012  msg=bank code does not exist.
→ mapped as CHOICE_BANK_CODE_NOT_FOUND
```

### What went wrong

| App sent | Meaning | Problem |
|----------|---------|---------|
| `accountType: 4` | PesaLink = **external** bank | Wrong rail for Choice |
| `bankCode: "46"` | **Choice Microfinance Bank** (from bank-codes list) | Not accepted as a PesaLink bank by Hakikisha |

`GET /bank-codes` se `46` dikhna **kaafi nahi** — woh list mix hai (banks + fintech + SACCO + M-PESA + CIC…).  
**Har list entry PesaLink (`accountType = 4`) pe kaam nahi karti.**

---

## 2. Golden rule

```
Choice / Vyce / own Choice account  →  accountType = 0
Other Kenyan bank (KCB, Equity, …) →  accountType = 4 + that bank’s bankCode
M-Pesa number / paybill / till      →  accountType = 3 / 1 / 2
```

**Never** send `accountType: 4` with `bankCode: "46"` (Choice Microfinance).

---

## 3. How to map UI → API

### Screen options (recommended)

| User selects in app | `accountType` | `bankCode` on validate | `payeeBankCode` on send |
|---------------------|---------------|------------------------|-------------------------|
| **Choice / Vyce account** | `0` | omit | optional / product default (not `"46"` as PesaLink) |
| **Other bank** (bank picker) | `4` | selected bank’s code (`"01"`, `"68"`, …) | **same** code |
| **M-Pesa mobile** | `3` | omit | `"M-PESA"` (or list’s M-PESA code) |
| **Paybill** | `1` | omit | `"M-PESA"` |
| **Till** | `2` | omit | `"M-PESA"` |

### Bank picker filter (important)

For **“Other bank / PesaLink”** picker:

- Prefer classic banks that support PesaLink (examples from list): `"01"` KCB, `"02"` StanChart, `"03"` Absa, `"11"` Co-op, `"31"` Stanbic, `"57"` I&M, `"63"` DTB, `"68"` Equity, …
- **Do not** treat as PesaLink destination:
  - `"46"` Choice Microfinance Bank → use **Choice rail (`0`)** instead
  - `"M-PESA"` / `"AIRTEL"` → M-Pesa rails (`1`/`2`/`3`)
  - `CIC****`, `SIC****`, random fintech codes → only if Choice confirms they work on type `4`; otherwise hide or handle as product decides

If user picks Choice from a single “all banks” list, app must set **`accountType = 0`**, not `4`.

---

## 4. Correct request examples

### A) Choice / internal account (FIX for the failed case)

**Validate** — `POST /api/v1/transactions/validate-account`

```json
{
  "accountId": "46012001327585",
  "accountType": 0
}
```

**Send** — `POST /api/v1/transactions/send`

```json
{
  "payeeAccountId": "46012001327585",
  "accountType": 0,
  "payeeBankCode": "46",
  "amount": 10.0,
  "remark": "Choice transfer"
}
```

> Note: For type `0`, validate does **not** need `bankCode`. On send, `payeeBankCode` may still be sent for product/UI consistency — confirm with backend/product if your send path requires a non-blank value; never use type `4` for this account.

### B) External bank — PesaLink (e.g. Equity)

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
  "amount": 500.0
}
```

### C) M-Pesa mobile

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

---

## 5. Error codes to handle

| Code | Meaning | What app should do |
|------|---------|-------------------|
| `CHOICE_BANK_CODE_NOT_FOUND` / Choice `14012` | Bank code not valid for this rail (often type `4` + wrong code) | Check mapping: Choice → type `0`; other bank → type `4` + correct code |
| `INVALID_ACCOUNT_TYPE` | Missing / bad `accountType` | Always send integer 0–5 |
| `BANK_CODE_REQUIRED` | Type `4` without `bankCode` | Force bank selection |
| `ACCOUNT_FROZEN` / `ACCOUNT_RESTRICT_IN` | Recipient cannot receive | Block send; show message |
| `INVALID_ACCOUNT_ID` | Empty / bad account | Ask user to fix input |

---

## 6. Checklist for mobile PR

- [ ] Separate rails in UI: **Choice** vs **Other bank** vs **M-Pesa**
- [ ] Choice account → validate/send with **`accountType: 0`**
- [ ] Other bank → **`accountType: 4`** + `bankCode` / `payeeBankCode` from picker
- [ ] Never send `accountType: 4` + `bankCode: "46"`
- [ ] Same `accountType` on validate and send
- [ ] Handle `CHOICE_BANK_CODE_NOT_FOUND` with a clear “check bank / rail” message
- [ ] Do not assume every `GET /bank-codes` entry works for PesaLink

---

## 7. One-liner for the team

**`46` = Choice → use type `0`. Type `4` only for other banks’ PesaLink codes — not for Choice itself.**
