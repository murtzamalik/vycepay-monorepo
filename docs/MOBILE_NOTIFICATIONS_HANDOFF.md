# VycePay Mobile Notifications Handoff

**Audience:** Mobile team (Android first; iOS can follow the same contract)  
**Purpose:** Integrate **FCM tray pushes** + **in-app notification inbox** against the VycePay BFF.  
**Base URL:** BFF only (never call callback-service ports directly). Example: `https://<bff-host>`

> Auth / KYC / wallet / transfer flows stay the same. This document covers **notifications only**.

Related docs:
- [PUSH_NOTIFICATIONS.md](PUSH_NOTIFICATIONS.md) — backend FCM matrix and payload fields
- [MOBILE_AUTH_HANDOFF.md](MOBILE_AUTH_HANDOFF.md) — login + FCM token bind on verify-otp / login

---

## 1. What shipped (backend ready)

| Capability | Status | Mobile action |
|------------|--------|---------------|
| FCM push on Choice events (tx, KYC, statement, account) | Live | Show tray + deep link |
| Admin custom push (`ADMIN_MESSAGE`) | Live | Show tray + open inbox detail |
| In-app inbox API (list / unread / mark read / delete) | Live | **New screens** |
| Delivery logging / admin resend | Live | Mobile ignores |

**Important product rule:** Inbox is the **source of truth**. FCM is best-effort. If the user opens the app with no tray tap, they must still see messages in **Inbox**.

---

## 2. Product rules (must follow)

| Rule | Detail |
|------|--------|
| Entry | BFF only + `Authorization: Bearer <JWT>` |
| Do not send | `X-Customer-Id` — BFF injects it from JWT |
| FCM bind | Send `fcmToken` + `platform` on signup `verify-otp` **or** successful PIN `login` |
| Logout | `POST /api/v1/auth/logout` clears FCM targets — do not call `/auth/devices` |
| Inbox id | Use `id` (= UUID `publicId`) from inbox APIs — **not** numeric DB id |
| Soft delete | `DELETE` hides from inbox; does not “unsend” history on server forever for ops |
| Mark read | Call mark-read when user opens a notification (list tap or detail) |
| Badge | Poll or refresh `unread-count` on home resume / after push |
| Balance / inbound credit | Types `0002` and `0003` share `TRANSACTION_RESULT`; deduped by Choice `txId`. Prefer `data.externalId` for detail; fallback Choice `txId` on `GET /transactions/{id}` |

---

## 3. Screens to build

### 3.1 Home / App bar — notification bell

| Element | Behavior |
|---------|----------|
| Bell icon | Opens **Inbox list** |
| Badge | Show count from `GET /api/v1/notifications/unread-count` when `unreadCount > 0` |
| Refresh | On resume / after login / after opening inbox |

### 3.2 Inbox list (new)

| UI | Spec |
|----|------|
| Title | Notifications |
| Rows | `title`, `body` (1–2 lines), relative `createdAt`, unread style if `read == false` |
| Empty | “No notifications yet” |
| Pagination | Load more when scrolling (`page` / `size`) |
| Pull to refresh | Re-fetch page 0 |
| Tap row | Open **detail** (or deep-link destination) + call mark-read |
| Swipe / overflow delete | Soft-delete via API; remove from list |

### 3.3 Inbox detail (new)

| UI | Spec |
|----|------|
| Show | `title`, full `body`, `createdAt` |
| CTA | Based on `pushType` + `data` (same as FCM deep link table below) |
| On open | `PATCH .../read` if `read == false` |

### 3.4 System tray (existing — improve)

Keep FCM tray. On tap:

1. Open app cold/warm with extras from FCM `data`
2. Prefer navigating by `pushType` (section 6)
3. Optionally open inbox detail if you store/pass inbox `notificationId` (admin compose includes `notificationId` in data)

---

## 4. Auth headers

| Header | When |
|--------|------|
| `Content-Type: application/json` | All JSON |
| `Authorization: Bearer <token>` | **All** notification inbox APIs |

Public? **None** — inbox requires login.

Do **not** send `X-Customer-Id` from the app.

---

## 5. Inbox APIs (consume these)

All paths relative to BFF base.

### 5.1 List inbox

`GET /api/v1/notifications?page=0&size=20`

| Query | Default | Notes |
|-------|---------|-------|
| `page` | `0` | 0-based |
| `size` | `20` | Max `50` |

**Success (`NOTIFICATIONS_OK`):**

```json
{
  "success": true,
  "code": "NOTIFICATIONS_OK",
  "message": "Notifications",
  "data": {
    "items": [
      {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "pushType": "TRANSACTION_RESULT",
        "notificationType": "0002",
        "title": "Money received",
        "body": "You received KES 100.00 from Jane",
        "data": {
          "pushType": "TRANSACTION_RESULT",
          "notificationType": "0002",
          "txId": "TX...",
          "txStatus": "8",
          "amount": "100.00",
          "currency": "KES"
        },
        "read": false,
        "readAt": null,
        "createdAt": "2026-08-01T10:15:30Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3
  }
}
```

Soft-deleted items are **not** returned.

### 5.2 Unread badge

`GET /api/v1/notifications/unread-count`

**Success (`NOTIFICATION_UNREAD_OK`):**

```json
{
  "success": true,
  "code": "NOTIFICATION_UNREAD_OK",
  "message": "Unread count",
  "data": {
    "unreadCount": 3
  }
}
```

### 5.3 Mark read

`PATCH /api/v1/notifications/{id}/read`

- `{id}` = inbox item `id` (UUID string)

**Success (`NOTIFICATION_READ_OK`):** `data` = same shape as one list item (with `read: true`).

### 5.4 Soft delete

`DELETE /api/v1/notifications/{id}`

**Success (`NOTIFICATION_DELETED`):** empty / void `data`.

### 5.5 Error codes (typical)

| HTTP | Code | Meaning |
|------|------|---------|
| 401 | `UNAUTHORIZED` | Missing/invalid JWT (or missing customer context) |
| 404 | `NOTIFICATION_NOT_FOUND` | Wrong id or already deleted / not owned |
| 404 | `CUSTOMER_NOT_FOUND` | JWT customer missing |

Handle like other BFF errors (`success: false`, show `message`).

---

## 6. `pushType` → navigation map

Use **`data.pushType`** (string). Same for FCM tap and inbox tap.

| `pushType` | Open screen | Keys in `data` |
|------------|-------------|----------------|
| `TRANSACTION_RESULT` | Transaction detail | Prefer `externalId` (Vyce UUID); fallback `txId` (Choice). Also `txStatus`, `amount`, `currency`, `paymentChannel`, `errorCode` |
| `STATEMENT_READY` | Statement / download flow | `jobId`, `fileUrl` (prefer refetch by `jobId` if URL expired) |
| `KYC_DOCUMENT_CHECK` | KYC status / documents | `onboardingRequestId`, `resultCode`, `profileCheckStatus` |
| `KYC_ONBOARDING_RESULT` | KYC / home (wallet ready if status `7`) | `onboardingRequestId`, `accountId`, `status` |
| `ACCOUNT_STATUS` | Home / account | `accountId`, `accountStatus`, `statusLabel` |
| `ADMIN_MESSAGE` | Inbox detail (or home) | `notificationId` (inbox UUID), optional custom keys |

All `data` values are **strings**.

Suggested deep links (align with existing app routes):

| pushType | Example |
|----------|---------|
| `TRANSACTION_RESULT` | `vycepay://transaction/{externalId}` (or Choice `txId`) |
| `STATEMENT_READY` | `vycepay://statement/{jobId}` |
| `KYC_*` | `vycepay://kyc` |
| `ACCOUNT_STATUS` | `vycepay://home` |
| `ADMIN_MESSAGE` | `vycepay://notifications/{notificationId}` |

---

## 7. FCM (tray) — still required

### 7.1 Register token

On signup `POST /api/v1/auth/verify-otp` **or** successful `POST /api/v1/auth/login`:

```json
{
  "fcmToken": "<firebase-installation-token>",
  "platform": "ANDROID"
}
```

Omit `fcmToken` if permission denied — app still works; no tray until next bind.

### 7.2 Permission (Android 13+)

Request `POST_NOTIFICATIONS` after login / first home. Create channel early (`vycepay_notifications` or your branded channel).

### 7.3 Payload shape

```json
{
  "notification": {
    "title": "...",
    "body": "..."
  },
  "data": {
    "pushType": "TRANSACTION_RESULT",
    "notificationType": "0002",
    "txId": "..."
  }
}
```

Handle:

- Foreground: show in-app banner **or** local notification; refresh unread count
- Background / killed: system tray; tap → deep link
- Prefer reading **`data`**, not only `notification` block (some OEMs drop fields)

### 7.4 Token refresh

On `onNewToken`, re-bind on next successful login **or** call your existing sync worker that posts token with the current JWT (do **not** invent a second device-binding model). Prefer login/`verify-otp` bind path for consistency.

### 7.5 Logout

`POST /api/v1/auth/logout` only — backend clears tokens.

---

## 8. Recommended client architecture

```
Home
 ├─ Bell + badge ──► NotificationInboxScreen (list API)
 │                      └─ NotificationDetailScreen (mark read + CTA)
 └─ FCM service ──► tray + deep link router (same pushType map)
```

| Layer | Suggestion |
|-------|------------|
| Network | Retrofit/Ktor to BFF `/api/v1/notifications/**` |
| State | ViewModel / repository; cache list in memory; badge as `StateFlow` |
| Local DB | Optional Room cache for offline list — **not required** for v1 |
| Sync | On FCM received (foreground): invalidate inbox + unread |

---

## 9. QA checklist

### FCM
- [ ] `google-services.json` present; package `com.vycepay`
- [ ] Login/verify with `fcmToken` succeeds
- [ ] Tray message arrives (foreground / background / killed)
- [ ] Tap opens correct screen by `pushType`
- [ ] Logout → no further pushes to old token

### Inbox
- [ ] List loads after a push / admin compose
- [ ] Unread badge matches `unread-count`
- [ ] Mark read clears badge / unread style
- [ ] Delete removes row; refresh stays gone
- [ ] Pagination works (`page=1`)
- [ ] 401 when logged out; no crash
- [ ] Another user’s notification id → 404

### Types
- [ ] `TRANSACTION_RESULT` → tx detail
- [ ] `STATEMENT_READY` → statement flow
- [ ] `KYC_*` → KYC
- [ ] `ADMIN_MESSAGE` → detail readable

---

## 10. Out of scope for mobile (do not build)

- Admin compose / resend UI
- Calling `/internal/v1/notifications/**`
- Parsing Choice Bank webhooks
- Firebase Admin / sending
- Preferring `/auth/devices` register/unregister for normal app flow

---

## 11. Quick endpoint cheat sheet

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/v1/notifications` | Bearer |
| GET | `/api/v1/notifications/unread-count` | Bearer |
| PATCH | `/api/v1/notifications/{id}/read` | Bearer |
| DELETE | `/api/v1/notifications/{id}` | Bearer |
| POST | `/api/v1/auth/verify-otp` (+ optional `fcmToken`) | Public |
| POST | `/api/v1/auth/login` (+ optional `fcmToken`) | Public |
| POST | `/api/v1/auth/logout` | Bearer |

---

## 12. Support / debug

If inbox empty but tray arrived: check JWT user matches the pushed customer; confirm deploy ran Flyway `V9` (`customer_notification` table).  
If no tray: check notification permission + `fcmToken` bound + `FIREBASE_ENABLED` on server.
