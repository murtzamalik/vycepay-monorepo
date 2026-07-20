# VycePay Push Notifications (FCM)

End-to-end push: Android registers FCM tokens via auth-service; Choice Bank callbacks in **callback-service** send typed pushes via Firebase Admin.

## Architecture

```
Android ──POST /api/v1/auth/devices──► auth-service ──► device_token (MySQL)
Choice Bank webhook ──► callback-service handlers ──► device_token lookup ──► FCM
```

- **Registration owner:** `vycepay-auth-service`
- **Sender:** `vycepay-callback-service` (`PushNotificationPort` / `FirebasePushAdapter`)
- **0003 (balance change):** intentionally **no push** — live traffic pairs it with **0002**; pushing both doubles notifications.

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
| POST | `/api/v1/auth/devices` | Body `{ "fcmToken", "platform": "ANDROID" }` → `DEVICE_REGISTERED` + `data.deviceId` |
| DELETE | `/api/v1/auth/devices/{deviceId}` | Unregister on logout |

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

2. **Register FCM token after login**
   - After OTP success (JWT saved): fetch `FirebaseMessaging.getInstance().token` → `POST /api/v1/auth/devices` with `{ "fcmToken": "...", "platform": "ANDROID" }`
   - Persist returned **`deviceId`** (EncryptedSharedPreferences)
   - Also register on cold start if already authenticated and token missing/changed
   - On `onNewToken`: re-register only if logged in

3. **Fix or replace `DeviceTokenSyncWorker`**
   - Wire Hilt WorkManager (`@HiltWorker`, `hilt-work`, `HiltWorkerFactory` in `VycepayApp`), **or** register via coroutine from the auth flow (simpler)

4. **Unregister on logout**
   - `DELETE /api/v1/auth/devices/{deviceId}` then clear local `deviceId` + token

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
   - [ ] Login → row in `device_token`
   - [ ] Firebase Console test message (foreground / background / killed)
   - [ ] Backend 0002 / 0015 push arrives with correct title/body
   - [ ] Tap opens correct screen
   - [ ] Logout → device deleted; no further pushes
   - [ ] Token refresh re-registers

### Mobile does **not** need to

- Implement Firebase Admin / sending
- Parse Choice Bank raw `notificationType` webhooks (backend maps to `pushType`)
- Store Choice Bank S3 credentials
