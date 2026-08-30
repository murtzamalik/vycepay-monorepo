# Mobile Team — Change Notice: M-PESA Paybill / Buy Goods (B2B)

**Audience:** Android / iOS  
**Date:** 2026-08-30  
**Status:** Backend shipped — mobile must update Paybill send payload  

Full screen guide: [MOBILE_MPESA_MOBILE_PAYBILL_TILL_HANDOFF.md](MOBILE_MPESA_MOBILE_PAYBILL_TILL_HANDOFF.md)

---

## What changed (for you)

| Area | Before | After |
|------|--------|--------|
| Endpoints | Same `validate-account` + `send` | **No new endpoints** |
| Paybill / Till send | Went through general transfer → Choice treated shortcode as **mobile** → `12010 Invalid mobile number` | Backend routes `accountType` **1** / **2** to Choice Paybill/Till API |
| Paybill **Account Number** (Acc / reference) | Often stuffed in `remark` only | **New required field:** `payeeReferenceNumber` on `POST /send` when `accountType` is `1` |
| Buy Goods (Till) | Same send shape | Still no reference field; `accountType: 2` only |

Mobile still talks **only to BFF**. Do not call Choice paths directly.

---

## What you must change in the app

### 1. Paybill screen (`accountType = 1`)

UI already has (or should have) two numbers:

| UI field | API field |
|----------|-----------|
| Business / Paybill number | `accountId` (validate) / `payeeAccountId` (send) |
| Account Number (e.g. Equity Acc `0820176326076`) | **`payeeReferenceNumber`** on **send only** |
| Amount | `amount` |
| Optional note | `remark` |

**Do not** put the Acc number only in `remark` — backend will reject Paybill send without `payeeReferenceNumber` (`INVALID_PAYEE_REFERENCE`).

### 2. Buy Goods / Till screen (`accountType = 2`)

No payload change beyond correct `accountType: 2`. Omit `payeeReferenceNumber`.

### 3. Send Money to Mobile (`accountType = 3`)

Unchanged.

---

## APIs (unchanged URLs)

Base: BFF only (e.g. `http://app.vycepay.com:9090`).

### Validate (both Paybill & Till)

`POST /api/v1/transactions/validate-account`  
Headers: `Authorization: Bearer <token>`

**Paybill**
```json
{
  "accountId": "247247",
  "accountType": 1
}
```

**Till**
```json
{
  "accountId": "987654",
  "accountType": 2
}
```

Show `accountName` from response. Block continue if error / frozen / restrict-in.

### Send

`POST /api/v1/transactions/send`  
Headers: `Authorization`, **`Idempotency-Key`** (new UUID every attempt)

**Paybill (required change)**
```json
{
  "payeeAccountId": "247247",
  "payeeBankCode": "M-PESA",
  "accountType": 1,
  "amount": 10.0,
  "payeeReferenceNumber": "0820176326076",
  "remark": "optional"
}
```

**Buy Goods**
```json
{
  "payeeAccountId": "987654",
  "payeeBankCode": "M-PESA",
  "accountType": 2,
  "amount": 50.0,
  "remark": "Buy goods"
}
```

OTP (if Choice requires): same as other transfers —  
`send-otp` / `confirm-otp` with `transactionId` = send response `externalId`.

---

## Errors to handle

| Code / situation | UI action |
|------------------|-----------|
| `INVALID_PAYEE_REFERENCE` | Paybill: ask for Account Number; map to `payeeReferenceNumber` |
| Envelope `message` from Choice / Vyce | Show as-is — no client remapping |
| `ACCOUNT_FROZEN` / `ACCOUNT_RESTRICT_IN` | Block send |

---

## Checklist

- [ ] Paybill tile always sends `accountType: 1`
- [ ] Buy Goods tile always sends `accountType: 2`
- [ ] Validate + send use the **same** shortcode / till + same `accountType`
- [ ] Paybill Acc / Account Number → `payeeReferenceNumber` on send
- [ ] New `Idempotency-Key` per send attempt
- [ ] Stop relying on `remark` alone for Paybill Acc number

---

## Out of scope for this change

- New mobile endpoints  
- Android/iOS code in this repo (wire against this contract)  
- Storing reference in transaction history UI (backend does not persist reference column yet)
