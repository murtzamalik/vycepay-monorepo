# Mobile Handoff — Kenya Feature Log (Backend Done)

**Audience:** Mobile team (Android / iOS)  
**Purpose:** Wire app changes for Kenya pilot feedback. Backend items below are **done**; remaining rows are **mobile-only** or **Choice/external**.  
**Base URL:** BFF only (do not call service ports directly).

**Out of scope for this round:** Airtel support, KYC image-quality gate, SMS to paybill receivers (Rose → Choice).

Related docs:

- [MOBILE_NOTIFICATIONS_HANDOFF.md](MOBILE_NOTIFICATIONS_HANDOFF.md) — inbox + FCM
- [MOBILE_MPESA_MOBILE_PAYBILL_TILL_HANDOFF.md](MOBILE_MPESA_MOBILE_PAYBILL_TILL_HANDOFF.md) — paybill send (`accountType=1`)
- [MOBILE_BENEFICIARIES_HANDOFF.md](MOBILE_BENEFICIARIES_HANDOFF.md) — save beneficiary
- [MOBILE_AUTH_HANDOFF.md](MOBILE_AUTH_HANDOFF.md) — login + OTP

---

## Summary matrix

| Kenya log item | Owner | Mobile action |
|----------------|--------|---------------|
| OTP still `123456` | Backend (done) + ops | Expect real SMS OTP in staging/prod; do not hardcode `123456` |
| Wrong username + PIN message | Backend OK | Show envelope `message` for `INVALID_CREDENTIALS` |
| Read / unread notifications | Backend APIs ready | Call mark-read; stop showing all as unread |
| Notification content (name + ref) | Backend (done) | Display `title`/`body`; use `data.counterparty` / `data.reference` |
| Save beneficiary when already saved | Mobile | Do not prompt if already saved / handle `200 UPDATED` |
| Utilities catalog (KPLC, water, …) | Backend catalog (done) | `GET .../utilities/billers` + paybill UX |
| Collapse recent transactions | Mobile only | UI toggle |
| Biometric login | Mobile only | Fix toggle / Masaki |
| Balance needs logout/login | Backend refresh API (done) | Pull-to-refresh → `POST .../refresh-balance` |
| Editable phone on deposit | Backend already OK | Unlock phone field if locked in UI |
| Selfie stuck | Mobile / SDK | Fix liveness flow |
| SMS to money receiver | Choice / Rose | No Vyce API |

---

## 1. OTP (security)

Backend no longer defaults `OTP_DEV_FIXED_CODE` to `123456`. Staging/prod must have:

- `OTP_DEV_FIXED_CODE` empty / unset  
- `SMS_ENABLED=true`  
- `MOBIWAVE_API_TOKEN` set  

**Mobile:** Treat OTP as user-entered from SMS. Do not ship a hardcoded `123456` bypass in release builds.

---

## 2. Login error message

Wrong username or PIN already returns:

| Field | Value |
|-------|--------|
| HTTP | `401` |
| `code` | `INVALID_CREDENTIALS` |
| `message` | `Incorrect username or PIN. Please try again.` |

**Mobile:** Always show API envelope `message` (Choice-message-first / catalog). Do not replace with a generic “Something went wrong”.

---

## 3. Notifications — read/unread + richer copy

### Mark read (existing APIs)

| Method | Path |
|--------|------|
| GET | `/api/v1/notifications?page&size` |
| GET | `/api/v1/notifications/unread-count` |
| PATCH | `/api/v1/notifications/{publicId}/read` |
| DELETE | `/api/v1/notifications/{publicId}` |

**Mobile must:**

1. Call `PATCH .../read` when the user opens a notification.  
2. Style unread vs read from `read` on list items.  
3. Refresh unread badge after mark-read.  
4. Month grouping (if desired) is **client-side** from `createdAt`.

See [MOBILE_NOTIFICATIONS_HANDOFF.md](MOBILE_NOTIFICATIONS_HANDOFF.md).

### Richer money notification body (backend change)

Success money pushes now prefer counterparty for **all** channels and append a reference.

Examples:

- `You received KES 30.00 from ROSE WUGHANGA MWALUKUKU. Ref: <externalId-or-txId>`
- `You sent KES 30.00 to DERRICK GWEHONA MUDAKI. Ref: <txId>`
- No name: `Deposit of KES 50.00 completed. Ref: <txId>`

Extra FCM / inbox `data` fields:

| Key | Meaning |
|-----|---------|
| `counterparty` | Sender/recipient name when Choice sent it |
| `reference` | Prefer Vyce `externalId`, else Choice `txId` |
| `externalId` | Vyce transaction id (when known) |
| `txId` | Choice transaction id |

**Mobile:** Show `body` as returned. Prefer `data.externalId` for transaction detail deep link.

---

## 4. Balance refresh (no more logout/login)

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/wallets/me` | Cheap read of `balance_cache` |
| POST | `/api/v1/wallets/me/refresh-balance` | Pull Choice `getAccountDetails`, update cache, return wallet |

Both return (JSON body; same shape as before, plus timestamp):

```json
{
  "choiceAccountId": "46012001327510",
  "balance": 322.00,
  "currency": "KES",
  "status": "ACTIVE",
  "lastBalanceUpdateAt": "2026-09-17T08:00:00Z"
}
```

**Mobile:**

1. Home / wallet screen: add pull-to-refresh and/or Refresh control.  
2. On refresh: `POST /api/v1/wallets/me/refresh-balance` (Bearer JWT; BFF injects `X-Customer-Id`).  
3. On normal open / resume: `GET /api/v1/wallets/me` is enough.  
4. Do not spam refresh (user gesture only).  
5. On error: show envelope `message`; keep last known balance on screen.

---

## 5. Kenya utilities / paybill catalog

### List billers

```http
GET /api/v1/transactions/utilities/billers
GET /api/v1/transactions/utilities/billers?category=ELECTRICITY
Authorization: Bearer <JWT>
```

Success envelope example:

```json
{
  "code": "UTILITY_BILLERS_OK",
  "message": "Kenya utility billers.",
  "data": {
    "categories": [
      { "id": "ELECTRICITY", "name": "Electricity" },
      { "id": "WATER", "name": "Water" },
      { "id": "HEALTHCARE", "name": "Healthcare & National Insurance" },
      { "id": "INTERNET", "name": "Internet and Telecommunications" }
    ],
    "billers": [
      {
        "id": "kplc-prepaid",
        "name": "Kenya Power (KPLC) — Prepaid Tokens",
        "category": "ELECTRICITY",
        "paybill": "888880",
        "alternatePaybills": [],
        "accountHint": "Meter number",
        "notes": "Use paybill 888880 with your meter number as the account number.",
        "accountType": 1
      }
    ]
  }
}
```

Billers included: KPLC prepaid/postpaid, NWSC, MOWASSCO, SHA, NSSF, Faiba (`330330` / alt `776611`), Zuku.

### How to pay (existing send flow)

Do **not** invent a new pay API. Use M-Pesa paybill send:

| Field | Value |
|-------|--------|
| `accountType` | `1` (from biller `accountType`) |
| `payeeAccountId` | biller `paybill` |
| `payeeReferenceNumber` | user-entered account / meter / ID (required) |
| `payeeBankCode` | `"M-PESA"` (from bank-codes) |

Validate → show title → amount → send → OTP as in [MOBILE_MPESA_MOBILE_PAYBILL_TILL_HANDOFF.md](MOBILE_MPESA_MOBILE_PAYBILL_TILL_HANDOFF.md).

**Mobile UI:** Categorize by `categories`; use `accountHint` as input placeholder; show `notes` if helpful.

---

## 6. Deposit — editable phone

`POST /api/v1/transactions/deposit/mpesa?mobile=&amount=` already accepts any MSISDN (not forced to registered phone).

**Mobile:** If the deposit screen locks the field to the logged-in number, make it editable (and validate Kenya format). Confirm STK goes to the entered number.

---

## 7. Pure mobile bugs / features (no backend change)

| Item | Guidance |
|------|----------|
| **Save beneficiary prompt** | After send (or before), if payee is already in beneficiaries list **or** create returns update/`200`, do not show “Save beneficiary?”. See [MOBILE_BENEFICIARIES_HANDOFF.md](MOBILE_BENEFICIARIES_HANDOFF.md). |
| **Collapse recent transactions** | Home UI toggle only; list still from `GET /api/v1/transactions`. |
| **Biometric login** | Device-local; clarify with Masaki. Backend has no biometric login API. |
| **Selfie stuck** | On-device camera / face liveness SDK; surface errors instead of infinite loading. |

---

## 8. Not for mobile this round

| Item | Why |
|------|-----|
| Airtel signup / Airtel Money | Explicitly deferred |
| KYC poor-image quality gate | Explicitly deferred |
| SMS to paybill/shortcode **receiver** | Choice / M-Pesa side (Rose). Vyce notifies the Vyce user via FCM only |

---

## 9. Suggested mobile sprint order

1. Balance refresh button / pull-to-refresh  
2. Notification mark-read + display new bodies  
3. Login error message display check  
4. Utilities catalog screen + paybill pay  
5. Beneficiary save prompt fix  
6. Deposit phone unlock  
7. Biometric / selfie / collapse TX as capacity allows  

---

## 10. Quick smoke checklist

- [ ] Staging OTP arrives by SMS (not always `123456`)  
- [ ] Wrong PIN shows “Incorrect username or PIN…”  
- [ ] Open notification → unread count decreases  
- [ ] Money notification shows name and/or `Ref:`  
- [ ] Pull-to-refresh updates balance without re-login  
- [ ] Utilities list loads 8 billers; paybill pay works for one biller  
- [ ] Deposit to alternate phone triggers STK on that number  
