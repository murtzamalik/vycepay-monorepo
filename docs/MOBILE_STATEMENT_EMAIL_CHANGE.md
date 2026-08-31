# Mobile Team — Change Notice: Account Statement via Email

**Audience:** Android / iOS  
**Date:** 2026-08-31  
**Status:** Backend shipped — mobile must update statement screen + payload  

Contract detail: [MOBILE_API_CONTRACT_DETAILED.md](MOBILE_API_CONTRACT_DETAILED.md) (statements section)

---

## What changed (for you)

| Area | Before | After |
|------|--------|--------|
| Endpoints | Same `apply` / `query` / `jobs/{id}` | **No new endpoints** — same BFF paths |
| Choice delivery | File ready via callback; app downloaded via `fileUrl` / `downloadUrl` | Statement **emailed** by Choice (encrypted link); **no** download URL in API |
| Apply body | `statementStartTime`, `statementEndTime`, optional `fileType` | **+ required `email`** |
| Push `STATEMENT_READY` | Deep link → download screen | **Do not rely on this for download** (ignore, or “check your email” only) |
| Query / job | Expect `statementUrl` / `downloadUrl` | Expect **`status`** + **`email`** only |

Mobile still talks **only to BFF**. Do not call Choice paths directly.

---

## What you must change in the app

### 1. Statement request screen — email field (mobile-owned UX)

| UI behaviour | Detail |
|--------------|--------|
| Prefill | Load registered email from **`GET /api/v1/wallets/account/details`** → field **`email`** (also on `GET /api/v1/auth/me`) |
| Editable | Prefer **read-only / display** of registered email — Choice delivers the statement to the **registered** address only |
| Required | Empty email → block submit (backend also returns `EMAIL_REQUIRED` / `INVALID_EMAIL`) |
| Profile | To change destination, use the normal email-update / verify flow first; then refresh account details |

Also collect:

| UI field | API field |
|----------|-----------|
| Destination email | **`email`** (required on apply) |
| Period start | `statementStartTime` (Unix ms) |
| Period end | `statementEndTime` (Unix ms) |
| Format (optional) | `fileType`: `0` = PDF, `1` = Excel |

### 2. Success / waiting UX

After successful apply:

- Show copy like: **“Your statement will be sent to {email}.”**
- Do **not** open an in-app PDF/Excel download or wait for `fileUrl`.
- Optional: poll `query` or `GET .../jobs/{jobId}` until `status` / `localStatus` is ready (`READY` / Choice `status: 1`), then confirm “sent / completed” — still no download URL.
- Inform user: email link from Choice **expires in 7 days**; file is encrypted (password: typically last 6 digits of mobile **or** ID number — Choice rules).

### 3. Remove download-centric flows

- Remove / stop using `downloadUrl`, `fileUrl`, `statementUrl` for this feature.
- Deep link `vycepay://statement/{jobId}` should **not** open a download screen; status / “check email” only, or remove the route.
- Push type `STATEMENT_READY`: treat as obsolete for this flow (no tap-to-download).

---

## APIs (unchanged URLs)

Base: BFF only (e.g. `http://app.vycepay.com:9090`).  
Headers: `Authorization: Bearer <token>` (BFF sets `X-Customer-Id`).

### Prefill source

`GET /api/v1/wallets/account/details` → `data.email` (Vyce registered email; Choice `getAccountDetails` does not return it).  
Fallback: `GET /api/v1/auth/me` → `email`.

Send that same address on apply — Choice emails the statement to the **registered** email only.

### Apply

`POST /api/v1/wallets/statements/apply`

```json
{
  "email": "customer@example.com",
  "statementStartTime": 1717200000000,
  "statementEndTime": 1722470400000,
  "fileType": 1
}
```

**Success (shape):** envelope `code` / `message` (prefer Choice `msg` as shown), `data` includes `jobId` / `choiceRequestId` and `email`.

### Query

`POST /api/v1/wallets/statements/query`

```json
{
  "requestId": "<jobId from apply>"
}
```

Choice-side fields typically include `jobId`, `accountId`, `email`, `status` (`0` = waiting, `1` = completed). **No** `statementUrl`.

### Local job

`GET /api/v1/wallets/statements/jobs/{choiceRequestId}`

Returns local `status` (`PENDING` / `READY` / …), `email`, and `downloadUrl` (null for new email jobs).

---

## Limits & errors to surface

| Rule / code | UI action |
|-------------|-----------|
| Period max **180 days** | Client-side validate; backend `INVALID_STATEMENT_PERIOD` |
| End date **today** | Cap `statementEndTime` at `min(endOfSelectedDay, now)` — never future midnight |
| Times | Keep sending **Unix ms** on BFF; backend converts to Choice seconds |
| Format | Prefer `fileType: 1` (Excel); Choice email statements are Excel-first |
| Max **10 requests / account / day** (Choice) | Show envelope `message` as-is |
| `EMAIL_REQUIRED` / `INVALID_EMAIL` | Fix email field |
| Envelope `message` from Choice / Vyce | Show as-is — no client remapping |
| `invalid end time` (Choice) | Usually bad/future end on client, or stale app against old backend; retry after backend deploy |

---

## Checklist

- [ ] Prefill email from `GET /api/v1/wallets/account/details` → `data.email`
- [ ] Prefer registered email only (read-only input unless profile email was updated)
- [ ] Send **`email`** on every `POST .../statements/apply`
- [ ] Enforce max 180-day date range in UI
- [ ] Success copy = emailed statement (not “tap to download”)
- [ ] Remove download-by-`fileUrl` / `STATEMENT_READY` download handling for statements
- [ ] Optional poll query/job for completion status only

---

## Out of scope for this change

- New BFF/wallet endpoint paths  
- Android/iOS code in this repo (wire against this contract)  
- Auto-forcing apply `email` to DB registered email server-side (mobile should send `data.email` from account details)
