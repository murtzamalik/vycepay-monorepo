# VycePay Mobile — KYC Biometric API Handoff

**Audience:** Android / iOS team  
**Base URL:** BFF only (same as all other mobile APIs)  
**Auth:** `Authorization: Bearer <JWT>` on every endpoint below  
**Do NOT** call WeVipers / BBS-SDKs (`http://2.28.204.253:8080`) from the app. VycePay owns that.

Related: [MOBILE_API_CONTRACT.md](MOBILE_API_CONTRACT.md), [MOBILE_API_CONTRACT_DETAILED.md](MOBILE_API_CONTRACT_DETAILED.md)

---

## 1. What changed for mobile

| Before | After |
|--------|--------|
| App called WeVipers directly (OCR, liveness, face match) | App calls **VycePay** `/api/v1/kyc/biometric/**` |
| No server-side journey logs on VycePay | Full session + attempt trail on backend |
| `POST /kyc/submit` with photos only | Submit may include `biometricSessionId`; photos optional if already stored in session |
| — | `GET /kyc/status` can return biometric fields |

**Unchanged after biometric PASS:** Choice OTP (`send-otp` / `confirm-otp`) and wallet poll.

---

## 2. Screen / call order (recommended)

```text
1. Auth register + OTP → JWT
2. GET  /api/v1/kyc/status                    (resume if biometricSessionId present)
3. POST /api/v1/kyc/biometric/sessions        (create or reuse session)
4. [OPTIONAL] document-liveness               (ID video)
5. POST .../ocr                               (ID front ± back) → prefill form
6. POST .../face-liveness                     (selfie video)
7. POST .../face-match                        (ID portrait crop + selfie photo)
8. When canSubmit == true:
     POST /api/v1/kyc/submit                  (+ biometricSessionId + username/pin + profile)
9. POST /api/v1/kyc/send-otp → confirm-otp    (unchanged)
10. Poll GET /api/v1/wallets/me
```

**Required biometric steps (backend gate):** `OCR` → `FACE_LIVENESS` → `FACE_MATCH`  
**Optional:** `DOCUMENT_LIVENESS` (endpoint exists; not required to reach `READY_TO_SUBMIT`)

---

## 3. Headers (all biometric + KYC)

```http
Authorization: Bearer <jwt>
Content-Type: application/json
```

- Do **not** send `X-Customer-Id` (BFF injects it from JWT).
- Payload size: images/videos as Base64 — allow large bodies (server accepts ~35 MB). Prefer JPEG for photos, MP4 for videos.
- Base64 may be raw or data-URI (`data:image/jpeg;base64,...`); server strips the prefix.

### Client timeouts (important)

| Call | Recommended client timeout |
|------|----------------------------|
| OCR | ≥ **60s** |
| Document / face liveness | ≥ **120s** |
| Face match | ≥ **30s** |
| Create/get session | 15–30s |
| Submit | 60s+ (may reload stored photos) |

BFF read timeout is **130s**.

---

## 4. Response shapes — read carefully

Two patterns:

### A) Success envelope (create session only)

```json
{
  "success": true,
  "code": "KYC_BIOMETRIC_SESSION_CREATED",
  "message": "Biometric session ready.",
  "requestId": "...",
  "data": { }
}
```

Use **`data`** for session fields.

### B) Direct JSON body (get session + all step APIs)

HTTP `200` with the object itself (no `success` / `data` wrapper).

**Business fail can still be HTTP 200** — always check `outcome`:

| `outcome` | Meaning | UI |
|-----------|---------|-----|
| `PASS` | Step OK | Go to `nextStep` or submit if `canSubmit` |
| `FAIL` | Hard fail (e.g. spoof) | Stop / support; often `sessionStatus=FAILED` |
| `RETRYABLE` | Soft fail | Show `message` / `failCode`, allow retry |
| `ERROR` | Unexpected | Retry or later |

### C) Error envelope (4xx/5xx)

```json
{
  "code": "KYC_BIOMETRIC_BAD_INPUT",
  "message": "Human-readable message",
  "requestId": "...",
  "details": null
}
```

Show `message` to the user. Use `code` for branching.

---

## 5. APIs

### 5.1 Create / resume session

`POST /api/v1/kyc/biometric/sessions`

**Request**

```json
{
  "idType": "101",
  "documentCountry": "KE",
  "forceNew": false
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `idType` | Recommended | Same as KYC: `101` National ID, `102` Alien, `103` Passport |
| `documentCountry` | Optional | Default server-side `KE` |
| `forceNew` | Optional | `true` cancels active session and creates new |

If an active `IN_PROGRESS` / `READY_TO_SUBMIT` session exists and `forceNew` is false, **same session is returned**.

**Response `200`** (envelope)

```json
{
  "success": true,
  "code": "KYC_BIOMETRIC_SESSION_CREATED",
  "message": "Biometric session ready.",
  "requestId": "…",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "IN_PROGRESS",
    "requiredSteps": ["OCR", "FACE_LIVENESS", "FACE_MATCH"],
    "expiresAt": "2026-10-27T18:00:00Z",
    "currentStep": null,
    "failReasonCode": null,
    "failReasonMsg": null,
    "canSubmit": false,
    "faceMatch": null,
    "faceSimilarity": null
  }
}
```

Persist `data.sessionId` for all later biometric calls and for submit.

---

### 5.2 Get session (resume UI)

`GET /api/v1/kyc/biometric/sessions/{sessionId}`

**Response `200`** (direct body)

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "requiredSteps": ["OCR", "FACE_LIVENESS", "FACE_MATCH"],
  "expiresAt": "2026-10-27T18:00:00Z",
  "currentStep": "OCR",
  "failReasonCode": null,
  "failReasonMsg": null,
  "canSubmit": false,
  "faceMatch": null,
  "faceSimilarity": null,
  "steps": [
    { "step": "OCR", "status": "PASS", "attempts": 1 },
    { "step": "FACE_LIVENESS", "status": "PENDING", "attempts": 0 },
    { "step": "FACE_MATCH", "status": "PENDING", "attempts": 0 }
  ],
  "ocrPreview": {
    "idNumber": "12345678",
    "fullName": "Jane Doe",
    "birthday": "1990-01-15",
    "gender": "M"
  }
}
```

`DOCUMENT_LIVENESS` appears in `steps` only if it was attempted.

**Session `status` values:**  
`IN_PROGRESS` | `READY_TO_SUBMIT` | `SUBMITTED` | `FAILED` | `EXPIRED` | `CANCELLED`

---

### 5.3 Document liveness (optional)

`POST /api/v1/kyc/biometric/sessions/{sessionId}/document-liveness`

**Request**

```json
{
  "videoBase64": "<mp4 base64 or data-uri>"
}
```

**Response `200`** (direct)

```json
{
  "attemptId": "…",
  "outcome": "PASS",
  "failCode": null,
  "retryable": false,
  "nextStep": "OCR",
  "sessionStatus": "IN_PROGRESS",
  "canSubmit": false,
  "message": "…",
  "livenessResult": "LIVE",
  "confidenceScore": 0.95
}
```

| `livenessResult` | Typical `outcome` | `failCode` |
|------------------|-------------------|------------|
| `LIVE` | `PASS` | — |
| `FAKE` | `FAIL` | `DOCUMENT_FAKE` |
| other / soft | `RETRYABLE` | `DOCUMENT_INCONCLUSIVE` |
| vendor 5xx | `RETRYABLE` | `VENDOR_5XX` |

---

### 5.4 OCR (required)

`POST /api/v1/kyc/biometric/sessions/{sessionId}/ocr`

**Request**

```json
{
  "documentKind": "KENYAN_ID",
  "frontImageBase64": "<jpeg base64>",
  "backImageBase64": "<jpeg base64 optional>"
}
```

| `documentKind` | Backend WeVipers mapping |
|----------------|--------------------------|
| `KENYAN_ID` (default) | Kenyan ID single / both |
| `KENYAN_PASSPORT` | Kenyan passport |
| `PAKISTANI_CNIC` | Pakistani CNIC |
| `AFGHAN_ID` | Afghan ID |
| `AFGHAN_LICENSE` | Afghan license |

Send both sides when you have them (`frontImageBase64` + `backImageBase64`).

**Response `200` PASS example**

```json
{
  "attemptId": "…",
  "outcome": "PASS",
  "failCode": null,
  "retryable": false,
  "nextStep": "FACE_LIVENESS",
  "sessionStatus": "IN_PROGRESS",
  "canSubmit": false,
  "message": "…",
  "detectedSide": "both",
  "extracted": {
    "name": "Jane Doe",
    "id_number": "12345678"
  }
}
```

Use `extracted` (and/or later `ocrPreview` on GET session) to **prefill** name / ID / DOB / gender. User can edit before submit.

Empty OCR / soft vendor errors → `outcome: RETRYABLE`, `failCode` e.g. `OCR_EMPTY` / `VENDOR_5XX`.

---

### 5.5 Face liveness (required)

`POST /api/v1/kyc/biometric/sessions/{sessionId}/face-liveness`

**Request**

```json
{
  "videoBase64": "<mp4 base64>"
}
```

**Requires** prior OCR `PASS`.

**Response `200` PASS**

```json
{
  "attemptId": "…",
  "outcome": "PASS",
  "failCode": null,
  "retryable": false,
  "nextStep": "FACE_MATCH",
  "sessionStatus": "IN_PROGRESS",
  "canSubmit": false,
  "message": "…",
  "livenessResult": "LIVE",
  "confidenceScore": 0.92,
  "kycRecommendation": "APPROVE"
}
```

| Case | `outcome` | `failCode` |
|------|-----------|------------|
| `LIVE` (+ APPROVE or missing rec) | `PASS` | — |
| `SPOOF` / non-APPROVE | `FAIL` | `FACE_SPOOF` |
| inconclusive | `RETRYABLE` | `FACE_INCONCLUSIVE` |
| vendor 5xx | `RETRYABLE` | `VENDOR_5XX` |

---

### 5.6 Face match (required)

`POST /api/v1/kyc/biometric/sessions/{sessionId}/face-match`

**Request**

```json
{
  "idPortraitBase64": "<cropped face from ID — NOT full card>",
  "selfieBase64": "<clear front-face selfie>"
}
```

**Requires** OCR + face-liveness `PASS`.  
VycePay does **not** crop — send the same portrait crop you previously sent to WeVipers.

**Response `200` PASS → ready to submit**

```json
{
  "attemptId": "…",
  "outcome": "PASS",
  "failCode": null,
  "retryable": false,
  "nextStep": null,
  "sessionStatus": "READY_TO_SUBMIT",
  "canSubmit": true,
  "message": "Faces match with high confidence.",
  "match": true,
  "similarity": 97.94
}
```

| Case | `outcome` | `failCode` | Notes |
|------|-----------|------------|--------|
| `match: true` | `PASS` | — | `canSubmit: true` |
| mismatch, retries left | `RETRYABLE` | `FACE_MISMATCH` | Retry selfie/portrait |
| mismatch, max attempts | `FAIL` | `FACE_MISMATCH` | `sessionStatus: FAILED` |
| bad crop / Rekognition | `RETRYABLE` | `BAD_CROP` | Fix portrait crop |
| vendor 5xx | `RETRYABLE` | `VENDOR_5XX` | Backoff retry |

Server match threshold is vendor-side (~80 similarity).

---

### 5.7 KYC status (extended)

`GET /api/v1/kyc/status`

**Response `200`** (existing fields + new nullable biometric fields)

```json
{
  "status": "NOT_STARTED",
  "displayStatus": "NOT_STARTED",
  "choiceOnboardingRequestId": null,
  "choiceAccountId": null,
  "message": null,
  "biometricSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "biometricStatus": "IN_PROGRESS",
  "biometricCanSubmit": false
}
```

On cold start: if `biometricSessionId` set → `GET .../sessions/{id}` and resume the right screen from `steps` / `canSubmit`.

---

### 5.8 Submit KYC (changed)

`POST /api/v1/kyc/submit`

**Request** — existing fields **plus**:

```json
{
  "biometricSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "jdoe",
  "pin": "1234",
  "firstName": "Jane",
  "middleName": "",
  "lastName": "Doe",
  "birthday": "1990-01-15",
  "gender": 1,
  "countryCode": "254",
  "mobile": "712345678",
  "idType": "101",
  "idNumber": "12345678",
  "frontSidePhoto": "",
  "backSidePhoto": "",
  "selfiePhoto": "",
  "address": null,
  "kraPin": null,
  "email": null
}
```

| Photos | Behavior |
|--------|----------|
| Empty / omitted | Backend loads from biometric session storage → Choice |
| Non-empty Base64 | Used as override and preferred for Choice |

**Always send `biometricSessionId`** once biometric flow is live in the app.

**Response `200`** (same as today)

```json
{
  "status": "1",
  "displayStatus": "PENDING",
  "choiceOnboardingRequestId": "ONBRD…",
  "choiceAccountId": null,
  "message": null
}
```

Then: `POST /api/v1/kyc/send-otp?onboardingRequestId=…` → `confirm-otp` (unchanged).

#### Feature flag (backend)

- `KYC_BIOMETRIC_REQUIRED=false` (current default): legacy submit without session still works.  
- When `true`: missing/incomplete session → errors below.

---

## 6. Error codes (biometric-related)

| HTTP | `code` | When | App action |
|------|--------|------|------------|
| 409 | `KYC_BIOMETRIC_REQUIRED` | Flag on; no session on submit | Run biometric flow first |
| 409 | `KYC_BIOMETRIC_NOT_READY` | Session not `READY_TO_SUBMIT` / wrong step order | Resume incomplete steps |
| 404 | `KYC_BIOMETRIC_SESSION_NOT_FOUND` | Bad `sessionId` | Create new session |
| 410 | `KYC_BIOMETRIC_SESSION_EXPIRED` | TTL (~24h) | `forceNew: true` create session |
| 400 | `KYC_BIOMETRIC_BAD_INPUT` | Empty/invalid Base64 | Re-capture |
| 502 | `KYC_BIOMETRIC_VENDOR_UNAVAILABLE` | Upstream down (catalog) | Retry later |
| 403 | `FORBIDDEN` | Session not owned by JWT user | Re-login |
| 404 | `CUSTOMER_NOT_FOUND` | Bad customer | Re-auth |

Also treat step-body `outcome` + `failCode` on HTTP 200 (section 4B / 5.x).

### Step `failCode` cheat sheet

| `failCode` | UX |
|------------|-----|
| `DOCUMENT_FAKE` | Hard stop |
| `DOCUMENT_INCONCLUSIVE` | Retry doc video |
| `FACE_SPOOF` | Hard stop |
| `FACE_INCONCLUSIVE` | Retry face video |
| `FACE_MISMATCH` | Retry selfie / ID portrait |
| `BAD_CROP` | Re-crop ID face (not full card) |
| `OCR_EMPTY` | Retake clearer ID photos |
| `VENDOR_5XX` | Retry 2–3× with 2–5s backoff |

---

## 7. Migration checklist (remove WeVipers from app)

- [ ] Delete WeVipers base URL / SDK HTTP client from app config  
- [ ] Replace all OCR / liveness / match calls with VycePay paths above  
- [ ] Store `sessionId`; pass `biometricSessionId` on submit  
- [ ] On HTTP 200 step responses, branch on `outcome` (not only HTTP status)  
- [ ] Portrait for match = **face crop**, same as before  
- [ ] Timeouts raised for video/OCR  
- [ ] After match `canSubmit=true`, submit → Choice OTP as today  
- [ ] Cold start: `GET /kyc/status` → resume biometric if needed  

---

## 8. Quick cURL examples (dev)

Replace `TOKEN` and `BASE` (BFF).

```bash
BASE=https://<bff-host>
TOKEN=eyJ...

# Create session
curl -s -X POST "$BASE/api/v1/kyc/biometric/sessions" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"idType":"101","documentCountry":"KE"}'

# OCR (sessionId from previous data.sessionId)
curl -s -X POST "$BASE/api/v1/kyc/biometric/sessions/$SID/ocr" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"documentKind\":\"KENYAN_ID\",\"frontImageBase64\":\"$FRONT_B64\"}"
```

---

## 9. QA acceptance

- [ ] Without WeVipers host in app, full path to `READY_TO_SUBMIT` works  
- [ ] Prefill from OCR `extracted` / `ocrPreview`  
- [ ] Face mismatch shows retry; spoof stops flow  
- [ ] Submit with empty photos + `biometricSessionId` still reaches Choice  
- [ ] Submit photo override still works  
- [ ] Expired session → create with `forceNew: true`  
- [ ] OTP + wallet poll unchanged after submit  

---

## 10. Support / debugging

Share with backend:

- JWT customer / mobile  
- `sessionId`  
- `attemptId` from last step response  
- BFF `requestId` from error envelope  
- Step name + `outcome` + `failCode`  

Admin portal: **KYC → detail** shows document gallery + biometric timeline (ops).

---

*Backend commit: biometric KYC via VycePay (`feat(kyc): route WeVipers biometric KYC through VycePay with audit trail`). Flag default off until this handoff is shipped in the app.*
