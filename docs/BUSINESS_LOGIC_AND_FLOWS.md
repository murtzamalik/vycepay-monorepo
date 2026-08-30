# VycePay — Business Logic and Flows

## 1. Authentication (Auth Service)

### Registration (signup)

1. Client: `POST /api/v1/auth/register` with `{ "mobileCountryCode": "254", "mobile": "712345678" }`.
2. **AuthFacade.sendSignupOtp:** Generates purpose=`SIGNUP` OTP, stores in `otp_verification`, logs/sends SMS.
3. Client: `POST /api/v1/auth/verify-otp` with mobile, otpCode, **imei** (required), optional fcmToken/platform.
4. **AuthFacade.verifySignupOtp:** Verifies SIGNUP OTP; creates `customer` if needed; **binds** single row in `customer_device`; optional FCM replace; returns JWT.
5. After KYC profile steps: Client `POST /api/v1/kyc/submit` with KYC fields **plus** `username` + 4-digit `pin`. KYC service sets credentials (BCrypt) in the same transaction before Choice Bank submit.

### Login (PIN + single device)

1. Client: `POST /api/v1/auth/login` with username **or** mobile, `pin`, `imei`, optional FCM.
2. If `pin_hash` null → send `CREDENTIALS_MIGRATE` OTP; return `mustSetCredentials` (no JWT).
3. Verify PIN (lockout after 5 fails / 15 min). If IMEI matches `customer_device` → JWT + FCM bind.
4. If IMEI mismatch/absent → send `DEVICE_BIND` OTP; return `deviceOtpRequired` (no JWT).
5. Client: `POST /api/v1/auth/verify-device-otp` → replace bound IMEI → **no JWT**; user returns to login and signs in again.

### Forgot PIN

1. `forgot-pin/request` → `PIN_RESET` OTP.
2. `forgot-pin/confirm` → verify OTP, set new PIN hash.

### Logout

1. Client: `POST /api/v1/auth/logout` with Bearer JWT.
2. **AuthFacade.logout:** Deletes all `device_token` rows for the customer. Client discards JWT.

**Identity:** JWT payload includes customer’s `externalId`. This is what the BFF puts in `X-Customer-Id` and what backends use to resolve `customer`.

**Monitoring:** Micrometer counters `auth.login.*`, `auth.pin.lockout`, `auth.device.bind`, `auth.pin.reset`, `auth.credentials.set`; rows in `auth_audit_event` (no secrets).

---

## 2. KYC Onboarding (KYC Service)

- **Precondition:** Authenticated (JWT → X-Customer-Id).

### Flow

1. **GET /api/v1/kyc/status**  
   Returns status for customer (e.g. NOT_STARTED, or status from `kyc_verification` and optional onboardingRequestId).

2. **POST /api/v1/kyc/submit**  
   Requires `username` + `pin` (app login). Ensures credentials on shared `customer` row (idempotent if same username already set), then builds Choice params from request (never includes app PIN); calls Choice Bank `onboarding/v3/submitEasyOnboardingRequest`. On success, creates/updates `kyc_verification` with `choice_onboarding_request_id`, status "1". Returns onboardingRequestId to client. Choice failure rolls back credentials.

3. **POST /api/v1/kyc/send-otp?onboardingRequestId=...**  
   Calls Choice `common/sendOtp` with businessId = onboardingRequestId, otpType = SMS.

4. **POST /api/v1/kyc/confirm-otp?onboardingRequestId=...&otpCode=...**  
   Calls Choice `common/confirmOperation`. Client then waits for wallet to appear (Choice processes asynchronously).

5. **Callback 0001 (Onboarding Result)**  
   Choice Bank POSTs to callback service. `OnboardingResultHandler` parses `params` (onboardingRequestId, status, userId, accountId, accountType, rejection info). Updates `kyc_verification`. If **status = 7** (account opened) and accountId present, **creates** `wallet` (customerId, choiceAccountId, balance 0, ACTIVE). So “wallet exists” only after 0001 with status 7.

6. **Polling:** Mobile can poll **GET /api/v1/wallets/me** until 200 (wallet created by callback).

---

## 3. Wallet (Wallet Service)

- **GET /api/v1/wallets/me**  
  Resolves customer by X-Customer-Id; loads wallet by customerId. Returns 404 until wallet exists (after KYC callback 0001). Response includes balance (from `balance_cache`) and `choiceAccountId`. Balance is updated by **callback 0003**, not by direct Choice API in this flow.

---

## 4. Send Money (Transaction Service)

- **Precondition:** Customer has wallet (KYC done, callback 0001 processed).

### Flow

0. **POST /api/v1/transactions/validate-account** (title fetch / Hakikisha)  
   Body: `accountId`, `accountType` (0–5), optional `bankCode` (required when `accountType` is 4 / PesaLink).  
   Calls Choice `account/validateAccount`. Returns `accountName`, `freezeStatus`, `restrictStatus`, `valid`. Frozen (`freezeStatus=1`) or restrict-in (`restrictStatus=1`) → error; do not proceed to send. Mobile shows title for user confirmation.

1. **POST /api/v1/transactions/send**  
   **Header:** `Idempotency-Key` (required). Body: `payeeBankCode`, `payeeAccountId`, **`accountType` (required)**, `amount`, optional `payeeReferenceNumber` (**required when `accountType` is 1 / Paybill**), `remark`; `payeeAccountName` is optional and **ignored** — server re-validates via Hakikisha and overwrites name from Choice.  
   **TransactionFacade.applyTransfer:** If transaction with same idempotencyKey exists, returns it. Otherwise re-calls `account/validateAccount` (for PesaLink uses `payeeBankCode` as `bankCode`), then Choice transfer: **`accountType` 1/2** → `trans/v2/applyForMpesaBusinessTransfer` (Paybill/Till); **other types** → `trans/v2/applyForTransfer`. Creates local `transaction` (externalId = new UUID, status PENDING, type TRANSFER) and saves. Response returns this transaction’s **externalId** (this is the “transactionId” used in subsequent OTP and status calls).

2. **If Choice requires OTP:**  
   - **POST /api/v1/transactions/send-otp?transactionId=<externalId>&otpType=SMS** → Choice `common/sendOtp` with businessId = transaction’s Choice-side id (or as configured).  
   - **POST /api/v1/transactions/confirm-otp?transactionId=<externalId>&otpCode=...** → Choice `common/confirmOperation`.

3. **Callback 0002 (Transaction Result)**  
   Choice POSTs to callback. `TransactionResultHandler` finds transaction by choiceTxId or choiceRequestId; updates status, errorCode, errorMsg, completedAt.

4. **GET /api/v1/transactions/{transactionId}/status**  
   Here `transactionId` is the **externalId** (UUID) from POST send. Returns current transaction status from DB (updated by 0002).

---

## 5. M-PESA Deposit (Transaction Service)

1. **POST /api/v1/transactions/deposit/mpesa?mobile=...&amount=...**  
   Optional header: `Idempotency-Key`. Resolves customer and wallet; calls Choice `trans/depositFromMpesa` (accountId, mobile, amount). Creates local `transaction` (type DEPOSIT, PENDING). If Idempotency-Key provided and a tx already exists with that key, returns it without calling Choice again.

2. **Callbacks:** 0002 updates transaction status (and pushes if first); 0003 updates wallet balance and may push if 0002 has not already notified the same `txId`. Unsolicited inbound Pay Bill credits (0003 only) upsert a local DEPOSIT and push.

---

## 6. Transaction List and History

- **GET /api/v1/transactions?page=&size=&status=&type=**  
  Local list from `transaction` table for customer (optional filters).

- **GET /api/v1/transactions/choice-history?startTime=&endTime=&page=&size=**  
  Calls Choice Bank API for transaction list (Choice-side).

- **GET /api/v1/transactions/bank-codes**  
  Returns bank codes from Choice for “send money” UI.

- **POST /api/v1/transactions/validate-account**  
  Hakikisha title fetch before send (see flow step 0 above).

---

## 7. Activity (Audit)

- **POST /api/v1/activity/log**  
  Body: action, resourceType, resourceId, etc. Stored in `activity_log` (customerId from X-Customer-Id).

- **GET /api/v1/activity**  
  Returns activity for customer (e.g. paginated).

---

## 8. Callback Processing (Summary)

| notificationType | Handler | Effect |
|------------------|--------|--------|
| 0001 | OnboardingResultHandler | Update kyc_verification; if status=7 create wallet |
| 0002 | TransactionResultHandler | Update transaction status (or upsert inbound DEPOSIT); TRANSACTION_RESULT push |
| 0003 | BalanceChangeHandler | Update wallet balance_cache; upsert inbound DEPOSIT if needed; TRANSACTION_RESULT push (deduped by txId) |
| UNKNOWN / other | UnknownNotificationHandler | Log only |

All callbacks are persisted in `choice_bank_callback` (raw payload, processed flag). Processing is async; HTTP response is 200 "ok" quickly.
