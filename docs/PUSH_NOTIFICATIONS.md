# VycePay Push Notifications (FCM)

End-to-end push: Android binds an FCM token on **verify-otp**; Choice Bank callbacks in **callback-service** send typed pushes via Firebase Admin.

## Architecture

```
Android ──POST /api/v1/auth/verify-otp (+ fcmToken)──► auth-service ──► device_token (MySQL)
Choice Bank webhook ──► callback-service handlers ──► device_token lookup ──► FCM
```

- **Registration owner:** `vycepay-auth-service` (via optional `fcmToken` on verify-otp)
- **Sender:** `vycepay-callback-service` (`PushNotificationPort` / `FirebasePushAdapter`)
- **One device:** each verify with `fcmToken` replaces all prior tokens for that customer
- **Logout:** `POST /logout` clears all `device_token` rows for the customer
- **0003 (balance change):** intentionally **no push** — live traffic pairs it with **0002**

## Backend configuration

| Property / env | Purpose |
|----------------|---------|
| `vycepay.firebase.enabled` / `FIREBASE_ENABLED` | `true` to send; default `false` (local/dev safe) |
| `FIREBASE_CREDENTIALS_JSON` | Service account JSON string (preferred secret) |
| `FIREBASE_CREDENTIALS_PATH` | Path to mounted service account file |
| (fallback) | Google Application Default Credentials |

Use the **same Firebase project** as the Android app (`com.vycepay`). Never commit the service account JSON.

## Device registration API

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/v1/auth/verify-otp` | **Primary (mobile):** optional `fcmToken`, `platform` — replaces token for customer |
| POST | `/api/v1/auth/logout` | Clears all FCM tokens for customer |
| POST | `/api/v1/auth/devices` | Optional / Postman / legacy |
| DELETE | `/api/v1/auth/devices/{deviceId}` | Optional / Postman / legacy |

## Callback → push matrix

| Choice `notificationType` | `pushType` | Title / body source | Customer resolution |
|---------------------------|------------|---------------------|---------------------|
| **0024** | `KYC_DOCUMENT_CHECK` | body = `params.resultDescription` | `onboardingRequestId` → KYC |
| **0001** | `KYC_ONBOARDING_RESULT` | status 7 → wallet ready; else rejection | `onboardingRequestId` → KYC |
| **0002** | `TRANSACTION_RESULT` | amount, txStatus 8/4, `errorMsg`, channel | `txId` → `transaction.customer_id` |
| **0015** / **0009** | `STATEMENT_READY` | fixed copy; `fileUrl` + `jobId` in data | statement job |
| **0021** | `ACCOUNT_STATUS` | mapped status label | `accountId` → wallet |
| **0003** | — | **No FCM** | — |

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
| `TRANSACTION_RESULT` | `txId`, `txStatus`, `amount`, `currency`, `paymentChannel`, `errorCode` |
| `STATEMENT_READY` | `jobId`, `fileUrl` (do not show URL in UI body; prefer API by jobId if URL expired) |
| `ACCOUNT_STATUS` | `accountId`, `accountStatus`, `statusLabel` |

All `data` values are strings.

## Mobile team checklist

See [Mobile handoff](#mobile-team-handoff) below — share that section with Android.

---

## Mobile team handoff

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
