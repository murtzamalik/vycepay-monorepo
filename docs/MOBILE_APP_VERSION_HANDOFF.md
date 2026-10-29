# VycePay Mobile — HTTPS base URL + app version gate

**Audience:** Mobile team  
**Purpose:** Point Play Store builds at HTTPS BFF, call the public app-config API on launch, and enforce force / soft update + maintenance screens.  
**Related:** [MOBILE_API_CONTRACT.md](MOBILE_API_CONTRACT.md), admin portal **System → App release**

> Backend and admin portal for version policy are already implemented. Ops edits min/latest/maintenance in the portal — the app only reads and enforces.

---

## 1. Why this change

| Context | Detail |
|---------|--------|
| Kenya Drive APK (current) | Talks to `http://app.vycepay.com:9090` — **leave working**; do not break it |
| Google Play requirement | Network traffic must use **HTTPS** |
| New public API host | `https://api.vycepay.com` (TLS terminated on nginx → same BFF `:9090`) |
| Version control | Ops raises min/latest in admin portal; app must gate on launch |

---

## 2. Base URL (flavors)

Use **BFF only**. Never call auth/kyc/wallet ports directly.

| Build | `BASE_URL` / `DC_BASE_URL` | Notes |
|-------|----------------------------|--------|
| **Play / prod release** | `https://api.vycepay.com/` | Port **443** only — no `:9090` |
| Drive / legacy emergency (optional) | `http://app.vycepay.com:9090/` | Only if you still ship a sideload flavor |
| UAT / local | Keep existing UAT override | e.g. `local.properties` / Gradle props |

### Required Android change (prod flavor)

Today prod is roughly `https://vycepay.com:9090/` — **wrong host/port for Play**.

Set prod to:

```text
https://api.vycepay.com/
```

Same value for `DC_BASE_URL` if KYC media still goes through BFF.

### Cleartext / network security (Play)

For **prod / Play** builds:

- Prefer `usesCleartextTraffic=false` (flavor-specific manifest if UAT still needs HTTP)
- Do **not** rely on cleartext for `api.vycepay.com`
- Certificate pinning: **leave off** for the first Play release (enable later when cert/host is stable)

---

## 3. Distribution flag

Add a BuildConfig (or equivalent) so update CTAs know where the user installed from:

| Value | Meaning |
|-------|---------|
| `play` | Open Play Store URL |
| `drive` | Prefer APK URL if present; else Play Store URL |

Suggested:

```kotlin
// prod Play: DISTRIBUTION = "play"
// optional Drive flavor: DISTRIBUTION = "drive"
buildConfigField("String", "DISTRIBUTION", "\"play\"")
```

Also send it as header `X-Distribution` on app-config (see below).

---

## 4. Public API — `GET /api/v1/auth/app-config`

**Auth:** none (no Bearer)  
**When:** cold start, **before** login / home navigation  
**Success code:** `APP_CONFIG_OK`

### Request headers

| Header | Required | Example | Notes |
|--------|----------|---------|--------|
| `X-App-Version-Code` | Strongly recommended | `12` | Android `versionCode` (int). Used for force/soft math |
| `X-App-Version-Name` | Optional | `1.0.3` | Logging / support |
| `X-Platform` | Optional | `android` | Default ANDROID if omitted |
| `X-Distribution` | Optional | `play` \| `drive` | Informational |
| `X-Request-Id` | Optional | UUID | Correlation |

### Example request

```http
GET /api/v1/auth/app-config HTTP/1.1
Host: api.vycepay.com
X-App-Version-Code: 12
X-App-Version-Name: 1.0.3
X-Platform: android
X-Distribution: play
```

### Example success response

```json
{
  "success": true,
  "code": "APP_CONFIG_OK",
  "message": "App configuration retrieved.",
  "requestId": "…",
  "data": {
    "platform": "ANDROID",
    "latestVersionCode": 15,
    "latestVersionName": "1.1.0",
    "minSupportedVersionCode": 10,
    "updateRequired": false,
    "updateRecommended": true,
    "updateTitle": "Update available",
    "updateMessage": "Please update VycePay to continue.",
    "storeUrl": "https://play.google.com/store/apps/details?id=com.wallet.vycepay",
    "apkUrl": null,
    "maintenance": false,
    "maintenanceMessage": null
  }
}
```

### How to interpret `data` (do not re-derive mins)

| Condition | UI |
|-----------|-----|
| `maintenance == true` | **MaintenanceScreen** — non-dismissible; show `maintenanceMessage` (or title/message fallback) |
| `updateRequired == true` | **ForceUpdateScreen** — non-dismissible; only Update (+ Retry) |
| `updateRecommended == true` (and not required) | Soft dialog — Update / Later |
| else | Continue normal splash → auth/home |

**Server already computed** `updateRequired` / `updateRecommended` from `X-App-Version-Code` vs portal policy. Trust the flags.

### Ops meaning (for your awareness)

| Portal control | Effect |
|----------------|--------|
| Raise **min supported version code** | Force update for older builds |
| Raise **latest version code** | Soft “update available” |
| **Maintenance** on | Block everyone |

---

## 5. Launch flow (required)

```text
App process start
  → system splash (optional)
  → GET /api/v1/auth/app-config  (timeout ~5–8s)
       ├─ maintenance     → MaintenanceScreen (block)
       ├─ updateRequired  → ForceUpdateScreen (block)
       ├─ updateRecommended → SoftUpdateDialog then continue
       └─ OK              → existing Splash / auth / home
```

Also re-check when returning from background after a long gap (e.g. ≥ 24h), or once per day — optional but useful.

Do **not** wait for JWT. This endpoint is public.

---

## 6. Update CTAs

### Force / soft “Update” button

| `DISTRIBUTION` | Prefer |
|----------------|--------|
| `play` | `storeUrl` — try `market://details?id=com.wallet.vycepay`, fallback to HTTPS Play link |
| `drive` | If `apkUrl` non-blank → open it; else `storeUrl` |

Package id: **`com.wallet.vycepay`** (prod; UAT may use `.uat` suffix — still use portal `storeUrl` as source of truth when present).

### Force screen extras

- Back disabled / no skip
- Secondary **Retry** → call app-config again (after user updated)
- Show `updateTitle` + `updateMessage`
- Optionally show “Latest: {latestVersionName} ({latestVersionCode})”

### Soft dialog

- Title/message from API
- **Update** / **Later**
- Remember dismiss for this `latestVersionCode` so you don’t nag every launch

### Maintenance screen

- Non-dismissible while `maintenance` stays true
- Retry button that re-fetches app-config

---

## 7. Offline / failure policy

| Situation | Behavior |
|-----------|----------|
| app-config network error / timeout, **no** prior force cached | **Fail open** — continue into app (do not brick users) |
| Last successful response had `updateRequired=true` or `maintenance=true` | Prefer **fail closed** using cache until a successful response clears it |
| Soft recommend only | Fail open is fine |

Cache last successful `data` JSON (e.g. encrypted prefs) with timestamp.

---

## 8. Retrofit sketch

```kotlin
interface AppConfigApi {
    @GET("api/v1/auth/app-config")
    suspend fun getAppConfig(
        @Header("X-App-Version-Code") versionCode: Int,
        @Header("X-App-Version-Name") versionName: String,
        @Header("X-Platform") platform: String = "android",
        @Header("X-Distribution") distribution: String,
    ): ApiSuccessResponse<AppConfigDto>
}

data class AppConfigDto(
    val platform: String?,
    val latestVersionCode: Int,
    val latestVersionName: String?,
    val minSupportedVersionCode: Int,
    val updateRequired: Boolean,
    val updateRecommended: Boolean,
    val updateTitle: String?,
    val updateMessage: String?,
    val storeUrl: String?,
    val apkUrl: String?,
    val maintenance: Boolean,
    val maintenanceMessage: String?,
)
```

Use `PackageManager` / `BuildConfig.VERSION_CODE` and `VERSION_NAME` for the headers.

Optional: OkHttp interceptor that adds `X-App-Version-Code` / `X-App-Version-Name` on **all** API calls for support logs (gate still uses dedicated endpoint).

---

## 9. versionCode discipline (Play)

| Rule | Detail |
|------|--------|
| Every Play upload | Increment `versionCode` (never reuse) |
| `versionName` | Human label (`1.0.0`, `1.0.1`, …) |
| After upload | Ops sets **Latest** in admin portal to that `versionCode` / name |
| Force retire old builds | Ops raises **Min** only after users can install the new build |

Coordinate with backend/ops before raising min, or Drive users on old APKs get stuck until they install the gated build.

---

## 10. Drive → Play migration note

- Signing keys may differ (Play App Signing vs Drive keystore). Users may need **uninstall + reinstall**, not in-place update.
- Reinstall can trigger **new device** OTP (IMEI / device bind). Support script should mention that.
- Until most users are on a build that calls app-config, **do not** ask ops to raise min above `1`.

---

## 11. Acceptance checklist (mobile)

- [ ] Prod Play build `BASE_URL` = `https://api.vycepay.com/`
- [ ] Health/smoke: login or `GET …/actuator/health` via HTTPS works on device
- [ ] Cold start calls `GET /api/v1/auth/app-config` **without** Bearer
- [ ] Headers include real `versionCode` / `versionName` / `DISTRIBUTION`
- [ ] Maintenance → blocking screen
- [ ] `updateRequired` → blocking ForceUpdate; Update opens store/APK correctly
- [ ] `updateRecommended` → dismissible soft prompt
- [ ] Network failure without prior force → app still opens
- [ ] Cleartext disabled (or scoped) for Play release
- [ ] UAT/Drive path (if kept) still documented and separate from Play flavor

---

## 12. Quick curl (for QA)

```bash
# Current policy + flags for versionCode 1
curl -sS 'https://api.vycepay.com/api/v1/auth/app-config' \
  -H 'X-App-Version-Code: 1' \
  -H 'X-App-Version-Name: 1.0.0' \
  -H 'X-Platform: android' \
  -H 'X-Distribution: play' | jq .

# After ops raises min in portal, same call with an old code should show updateRequired=true
```

Legacy Drive host (should keep working for old APKs):

```bash
curl -sS 'http://app.vycepay.com:9090/api/v1/auth/app-config' \
  -H 'X-App-Version-Code: 1' | jq .
```

---

## 13. Out of scope for mobile (backend/ops)

- Editing min/latest/maintenance → admin **System → App release**
- Hosting the APK binary → ops provides `apkUrl` string when ready
- nginx / TLS for `api.vycepay.com` → already live

Questions: backend team / this handoff owner.
