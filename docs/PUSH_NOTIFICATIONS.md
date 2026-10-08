# VycePay Push Notifications (FCM)

End-to-end push: Android binds an FCM token on **signup verify-otp** or **successful PIN login**; Choice Bank callbacks (and admin compose) in **callback-service** persist an inbox row, send FCM via Firebase Admin, and log each delivery attempt.

## Architecture

```
Android ──POST /api/v1/auth/verify-otp or /login (+ fcmToken)──► auth-service ──► device_token (MySQL)
Choice Bank webhook ──► handlers ──► NotificationOrchestrator ──► customer_notification + FCM + push_delivery_log (+ SMS for money events)
Admin compose/resend ──► admin-service ──► callback internal API ──► same orchestrator
Mobile inbox ──► BFF /api/v1/notifications/** ──► callback-service
```

- **Registration owner:** `vycepay-auth-service` (optional `fcmToken` on signup verify-otp **or** PIN login success)
- **Sender / inbox hub:** `vycepay-callback-service` (`NotificationOrchestrator`, `PushNotificationPort` / `FirebasePushAdapter`, MobiWave `SmsPort` for money events)
- **One FCM token:** each bind with `fcmToken` replaces all prior tokens for that customer
- **IMEI binding:** separate table `customer_device` (login device trust) — not the same as FCM
- **Logout:** `POST /logout` clears all `device_token` rows for the customer
- **Money events (0002 / 0003):** both can send `TRANSACTION_RESULT`; inbox is deduped by `TX:{txId}` so paired callbacks produce one notification, one FCM, and one SMS. Unsolicited inbound credits (Pay Bill) are covered by **0003** when no local tx exists. **FCM body** still comes from `PushMessageFactory`; **SMS body** is rendered from admin-editable `sms_template` rows (`TX_*` keys) via `SmsTemplateService` (soft-fail). Provider **FAILED** sends are parked in `sms_outbox` and retried by a callback-service job (OTP / admin bulk still use `sms_message` only).
- **Inbox:** `customer_notification` is source of truth; FCM and money SMS are best-effort delivery
- **Admin:** list/detail/summary via JDBC; compose (1–100 customers) and resend via internal API (`INTERNAL_API_KEY`)

### SMS templates vs FCM

System-triggered SMS (auth OTP + money events) uses seeded `sms_template` rows — one active body per key. Ops edit body/name/active in admin (`/sms/templates`); keys are not created/deleted in v1. Render chain: active DB body → compile-time `SmsTemplateDefaults` → generic fallback. **FCM/push copy is unchanged** (still `PushMessageFactory`). Admin **bulk SMS** remains freeform and is not in the template catalog.

**Admin bulk SMS audiences** (`POST /api/admin/v1/sms/bulk`, permission `sms:bulk`):

| Audience | Recipients | Delivery |
|----------|------------|----------|
| `MANUAL` (default) | Phone list in body (max 100) | Synchronous send; response includes sent/failed counts |
| `ALL_CUSTOMERS` | DB customers with status `ACTIVE` / `PENDING` / `SUSPENDED` (not `DEACTIVATED`), valid Kenya mobile; max 10_000 | Enqueue `sms_message` rows (`customer_id` set), return `status: ACCEPTED`, then async provider send |

Preview counts: `GET /api/admin/v1/sms/bulk/audience-preview` → `{ totalCustomers, withValidMobile, skippedInvalid }`. Track progress on SMS list filtered by `batchId` (PENDING → SENT/FAILED).

| Key prefix | Service | Examples |
|------------|---------|----------|
| `OTP_*` | auth-service | `OTP_SIGNUP`, `OTP_DEVICE_BIND`, `OTP_PIN_RESET`, `OTP_CREDENTIALS_MIGRATE` |
| `TX_*` | callback-service | `TX_PAY_TILL_SUCCESS`, `TX_INBOUND_SUCCESS`, `TX_FAILED`, `TX_DEFAULT_SUCCESS`, … |

API: `GET/PUT /api/admin/v1/sms/templates/{key}`, `POST .../preview` (`sms:view` / `sms:template:edit`).

## Backend configuration

| Property / env | Purpose |
|----------------|---------|
| `vycepay.firebase.enabled` / `FIREBASE_ENABLED` | `true` to send; default `false` (local/dev safe) |
| `FIREBASE_CREDENTIALS_JSON` | Service account JSON string (preferred secret) |
| `FIREBASE_CREDENTIALS_PATH` | Path to mounted service account file |
| `vycepay.sms.enabled` / `SMS_ENABLED` | `true` to send money-event SMS via MobiWave; default `false` (logging adapter) |
| `MOBIWAVE_API_TOKEN` / `MOBIWAVE_BASE_URL` / `MOBIWAVE_SENDER_ID` | MobiWave credentials (same as auth OTP) |
| `vycepay.sms.outbox.poll-interval-ms` / `SMS_OUTBOX_POLL_MS` | Outbox retry poll delay (default 60000) |
| `vycepay.sms.outbox.batch-size` / `SMS_OUTBOX_BATCH_SIZE` | Max rows per poll (default 50) |
| `vycepay.sms.outbox.max-attempts` / `SMS_OUTBOX_MAX_ATTEMPTS` | Attempts before DEAD (default 10) |
| `INTERNAL_API_KEY` | Shared secret for admin → callback compose/resend |
| `vycepay.bff.callback-url` / `BFF_CALLBACK_URL` | BFF routes `/api/v1/notifications/**` to callback-service |
| (fallback) | Google Application Default Credentials |

Use the **same Firebase project** as the Android app (`com.vycepay`). Never commit the service account JSON.


## Device registration API

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/v1/auth/verify-otp` | Signup: optional `fcmToken`, `platform` — replaces FCM token |
| POST | `/api/v1/auth/login` | On JWT success: optional `fcmToken` replaces FCM token |
| POST | `/api/v1/auth/logout` | Clears all FCM tokens for customer |
| POST | `/api/v1/auth/devices` | Optional / Postman / legacy |
| DELETE | `/api/v1/auth/devices/{deviceId}` | Optional / Postman / legacy |

## Callback → push matrix

| Choice `notificationType` | `pushType` | Title / body source | Customer resolution |
|---------------------------|------------|---------------------|---------------------|
| **0024** | `KYC_DOCUMENT_CHECK` | body = `params.resultDescription` | `onboardingRequestId` → KYC |
| **0001** | `KYC_ONBOARDING_RESULT` | status 7 → wallet ready; else rejection | `onboardingRequestId` → KYC |
| **0002** | `TRANSACTION_RESULT` | amount, txStatus 8/4, `errorMsg`, channel | `txId` → transaction (update or inbound upsert) |
| **0003** | `TRANSACTION_RESULT` | treated as success; Pay Bill / transfer copy | `accountId` → wallet; upsert inbound DEPOSIT if needed |
| **0015** / **0009** | `STATEMENT_READY` | fixed copy; `fileUrl` + `jobId` in data | statement job |
| **0021** | `ACCOUNT_STATUS` | mapped status label | `accountId` → wallet |

**Dedupe:** `customer_notification.dedupe_key = TX:{choiceTxId}` (unique per customer). Whichever of 0002/0003 arrives first wins; the pair is skipped with `ALREADY_NOTIFIED` (no second FCM or SMS).

### Money-event SMS outbox

When MobiWave returns **FAILED** on a money SMS, callback-service inserts/updates `sms_outbox` keyed by `TX:{txId}` and a scheduled job retries with exponential backoff (max attempts then `DEAD`). `SMS_DISABLED` / invalid mobile are **not** parked. This does **not** change OTP or admin bulk SMS (`sms_message`).

**Admin UI:** `/sms/outbox` (list + detail) under SMS; requires `sms:view`. Manual **Requeue** uses `sms:resend` and sets the row to `PENDING` for the next job poll. API: `GET /api/admin/v1/sms/outbox`, `GET /api/admin/v1/sms/outbox/{id}`, `POST /api/admin/v1/sms/outbox/{id}/retry`.

## FCM payload contract (Android)

```json
{
  "notification": {
    "title": "Your statement is ready",
    "body": "Tap to download your account statement."
  },
  "data": {
    "notificationType": "0015",
    "pushType": "STATEMENT_READY",
    "jobId": "DSJ...",
    "fileUrl": "https://..."
  }
}
```

### `data` fields by `pushType`

| pushType | Extra data keys |
|----------|-----------------|
| `KYC_DOCUMENT_CHECK` | `onboardingRequestId`, `resultCode`, `profileCheckStatus` |
| `KYC_ONBOARDING_RESULT` | `onboardingRequestId`, `accountId`, `status` |
| `TRANSACTION_RESULT` | `txId`, `externalId` (Vyce UUID), `txStatus`, `amount`, `currency`, `paymentChannel`, `errorCode` |
| `STATEMENT_READY` | `jobId`, `fileUrl` (do not show URL in UI body; prefer API by jobId if URL expired) |
| `ACCOUNT_STATUS` | `accountId`, `accountStatus`, `statusLabel` |

All `data` values are strings.

## Mobile team checklist

Share **[MOBILE_NOTIFICATIONS_HANDOFF.md](MOBILE_NOTIFICATIONS_HANDOFF.md)** with Android — full inbox APIs, screens, deep links, and QA.

See also the shorter checklist below.

---

## Mobile team handoff

**Full guide:** [MOBILE_NOTIFICATIONS_HANDOFF.md](MOBILE_NOTIFICATIONS_HANDOFF.md)

**Package / Firebase Android app ID:** `com.vycepay`

### Must do

1. **Add `google-services.json`**
   - Firebase Console → Android app `com.vycepay` → download → `vycepay-android/app/google-services.json`
   - Same Firebase project as the backend service account
   - Do not commit if policy forbids; supply via CI/secure channel

2. **Send FCM token on verify-otp (not on login OTP send)**
   - On `POST /api/v1/auth/verify-otp`, when available:
     ```json
     {
       "mobileCountryCode": "254",
       "mobile": "712345678",
       "otpCode": "123456",
       "fcmToken": "<firebase-token>",
       "platform": "ANDROID"
     }
     ```
   - Do **not** call `POST /auth/devices` or `DELETE /auth/devices/{deviceId}`
   - If permission denied / token unavailable: omit `fcmToken` — login still works

3. **Logout**
   - Call `POST /api/v1/auth/logout` only — backend clears the push target

4. **FCM rotation while logged in**
   - Handled by mobile (re-send on next verify or your chosen mechanism)

5. **Runtime notification permission (Android 13+)**
   - Request `POST_NOTIFICATIONS` after login / first home
   - Ensure notification channel exists early

6. **Handle FCM payloads**
   - Support notification+data and data-only messages
   - Navigate by `data.pushType`:
     - `TRANSACTION_RESULT` → transaction detail (`txId`)
     - `STATEMENT_READY` → download via API/`jobId` (prefer over raw `fileUrl` if expired)
     - `KYC_*` → KYC status / home
     - `ACCOUNT_STATUS` → home / settings
   - Use a proper small notification icon

7. **Deep links**
   - Wire `vycepay://transaction/{id}` in Compose Navigation; map notification tap extras the same way

8. **QA**
   - [ ] Build with `google-services.json`
   - [ ] verify-otp with `fcmToken` → one row in `device_token`
   - [ ] Second login with new token → still one row, updated token
   - [ ] Firebase Console test message (foreground / background / killed)
   - [ ] Backend 0002 / 0015 push arrives with correct title/body
   - [ ] Tap opens correct screen
   - [ ] Logout → no `device_token` rows; no further pushes

### Mobile does **not** need to

- Call `/auth/devices` register or unregister
- Persist `deviceId`
- Implement Firebase Admin / sending
- Parse Choice Bank raw `notificationType` webhooks (backend maps to `pushType`)
- Store Choice Bank S3 credentials
