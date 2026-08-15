# Mobile Team — Contacts Verify Handoff

**Audience:** Android / iOS  
**Purpose:** Match device address-book numbers to VycePay users who have an **ACTIVE** wallet (fully onboarded). Show username + account title; use `payeeAccountId` later for Choice transfer (`accountType: 0`).  
**Base URL:** BFF only (never call auth-service ports directly). Example: `http://app.vycepay.com:9090`

Related:

- [MOBILE_AUTH_HANDOFF.md](MOBILE_AUTH_HANDOFF.md) — login / JWT
- [MOBILE_VALIDATE_ACCOUNT_HANDOFF.md](MOBILE_VALIDATE_ACCOUNT_HANDOFF.md) — validate + send
- [MOBILE_CHOICE_VS_PESALINK.md](MOBILE_CHOICE_VS_PESALINK.md) — Choice (`accountType: 0`) vs PesaLink (`4`)
- [MOBILE_ACCOUNT_TYPE_INSTRUCTIONS.md](MOBILE_ACCOUNT_TYPE_INSTRUCTIONS.md) — `accountType` 0–5

---

## 1. Product rules (must follow)

| Rule | Detail |
|------|--------|
| Show only matches | UI list = `data.matches` only. Do not invent “not on VycePay” rows from the full address book unless product asks for that later. |
| Eligibility | Backend returns a contact only if: phone matches **and** username set **and** wallet `ACTIVE` (Choice account opened). |
| Self | Own number is never returned. |
| Account title | `accountTitle` = `firstName` + `lastName` from VycePay (not Choice Hakikisha). |
| Send money | **Not wired in this API.** When product enables send from this list, use Choice rail: `accountType: 0`, `payeeAccountId` from match, then existing validate + send (see Choice docs). |
| Privacy | Do not log full contact lists. Request over BFF with JWT only. |
| Batch size | Max **500** contacts per request. Chunk larger address books. |

---

## 2. Screen flow (app-owned)

```
1. Request READ_CONTACTS (OS permission)
2. Read local contacts → collect mobile dial strings (raw is OK)
3. Chunk to ≤500 → POST /api/v1/auth/contacts/verify
4. Render matches: title = accountTitle (fallback username), subtitle = username
5. On tap → (later) validate-account + send with accountType 0 + payeeAccountId
```

Mobile team owns permission UX and list UI. Backend only verifies.

---

## 3. Auth headers

| Header | When |
|--------|------|
| `Content-Type: application/json` | Always |
| `Authorization: Bearer <token>` | Required |

Do **not** send `X-Customer-Id` — BFF sets it from JWT.

---

## 4. API

`POST /api/v1/auth/contacts/verify`

### 4.1 Request

```json
{
  "contacts": [
    { "mobile": "712345678" },
    { "mobile": "+254798765432" },
    { "mobile": "254798765432" },
    { "mobile": "0798765432" }
  ]
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `contacts` | Yes | Non-empty array, max 500 items |
| `contacts[].mobile` | Yes per item | Raw dial string; backend normalizes |

Invalid / unparseable numbers are **skipped** (do not fail the whole batch).

### 4.2 Normalization (backend owns this)

Kenya-first; stored as `mobileCountryCode=254` + 9-digit national (no leading `0`).

| App may send | Matched as |
|--------------|------------|
| `712345678` | `254` / `712345678` |
| `0712345678` | `254` / `712345678` |
| `254712345678` | `254` / `712345678` |
| `+254712345678` | `254` / `712345678` |
| spaces / dashes | stripped before parse |

App does **not** need to normalize before send; sending raw contact strings is fine.

### 4.3 Success response

HTTP `200`, success envelope code **`AUTH_CONTACTS_VERIFIED`**:

```json
{
  "success": true,
  "code": "AUTH_CONTACTS_VERIFIED",
  "message": "Contacts verified.",
  "requestId": "...",
  "data": {
    "matches": [
      {
        "inputMobile": "0798765432",
        "mobileCountryCode": "254",
        "mobile": "798765432",
        "username": "jane_doe",
        "accountTitle": "Jane Doe",
        "customerExternalId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "payeeAccountId": "46012001327585"
      }
    ]
  }
}
```

| Field | Use |
|-------|-----|
| `inputMobile` | Original string from this request chunk (for correlating UI rows) |
| `mobileCountryCode` / `mobile` | Normalized key |
| `username` | Display / identity |
| `accountTitle` | Primary display name |
| `customerExternalId` | Public customer UUID (not for Choice send) |
| `payeeAccountId` | Choice account id — use later as send `payeeAccountId` with `accountType: 0` |

No matches → `data.matches: []` (still `200` / `AUTH_CONTACTS_VERIFIED`).

### 4.4 Errors

| HTTP | Code | When |
|------|------|------|
| `401` | (auth) | Missing / invalid JWT |
| `400` | `INVALID_CONTACTS` | Missing/empty `contacts`, or size > 500 |
| `404` | `CUSTOMER_NOT_FOUND` | Caller not found |
| `429` | `RATE_LIMITED` | Too many verify calls (policy: 30 / 60s per user) |

---

## 5. Chunking

If the address book has more than 500 numbers:

1. Split into chunks of ≤500  
2. Call verify for each chunk  
3. Merge `matches` by `customerExternalId` or normalized `mobileCountryCode`+`mobile`  
4. Dedupe before showing the list  

---

## 6. Later: send from a match (not implemented in this handoff)

When product enables pay-from-contacts (Choice only):

1. `POST /api/v1/transactions/validate-account`  
   `{ "accountId": "<payeeAccountId>", "accountType": 0 }`  
2. `POST /api/v1/transactions/send` + `Idempotency-Key`  
   `{ "payeeAccountId": "<payeeAccountId>", "accountType": 0, "payeeBankCode": "46", "amount": "...", "remark": "..." }`  
3. OTP confirm if Choice requires it  

Do **not** use `accountType: 4` / PesaLink for these payees. See [MOBILE_CHOICE_VS_PESALINK.md](MOBILE_CHOICE_VS_PESALINK.md).

---

## 7. QA checklist

- [ ] JWT required; unauthenticated → 401  
- [ ] Mixed formats (`07…`, `+254…`, `254…`, bare `7…`) all match the same user when registered as `254` + national  
- [ ] User without wallet / suspended wallet not returned  
- [ ] User without username not returned  
- [ ] Own number not returned  
- [ ] Empty `matches` when none registered  
- [ ] `contacts` empty or >500 → `INVALID_CONTACTS`  
- [ ] List UI shows `accountTitle` + `username`  
- [ ] Chunking works for &gt;500 contacts  
- [ ] No full contact list in app logs  

---

## 8. Curl example

```bash
curl -s -X POST "$BFF/api/v1/auth/contacts/verify" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "contacts": [
      { "mobile": "712345678" },
      { "mobile": "+254798765432" },
      { "mobile": "0798765432" }
    ]
  }'
```
