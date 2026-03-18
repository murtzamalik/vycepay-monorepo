# VycePay — Business Logic and Flows

## 1. Authentication (Auth Service)

### Registration

1. Client: `POST /api/v1/auth/register` with `{ "mobileCountryCode": "254", "mobile": "712345678" }`.
2. **AuthFacade.sendOtp:** Generates OTP, stores in `otp_verification` (expiry e.g. 5 min), “sends” (default: logs; production would use SMS port).
3. Client: `POST /api/v1/auth/verify-otp` with `{ "mobileCountryCode", "mobile", "otpCode" }`.
4. **AuthFacade.verifyOtpAndGetToken:** Verifies OTP (latest for that mobile); if valid, finds or **creates** `customer` (externalId = UUID, status ACTIVE), then issues JWT with customer id and externalId. Returns token and externalId in response.

### Login (existing customer)

1. Client: `POST /api/v1/auth/login` with same body as register.
2. **AuthFacade.login:** Throws if customer not found; otherwise sends OTP (same as register).
3. Client: Same verify-otp as above; gets new JWT.

**Identity:** JWT payload includes customer’s `externalId`. This is what the BFF puts in `X-Customer-Id` and what backends use to resolve `customer` (e.g. `customerRepository.findByExternalId(externalId)`).

---

## 2. KYC Onboarding (KYC Service)

- **Precondition:** Authenticated (JWT → X-Customer-Id).

### Flow

1. **GET /api/v1/kyc/status**  
   Returns status for customer (e.g. NOT_STARTED, or status from `kyc_verification` and optional onboardingRequestId).

2. **POST /api/v1/kyc/submit**  
   Builds Choice params from request (e.g. name, id type, id number, phone); calls Choice Bank `onboarding/v3/submitEasyOnboardingRequest`. On success, creates/updates `kyc_verification` with `choice_onboarding_request_id`, status "1". Returns onboardingRequestId to client.

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

1. **POST /api/v1/transactions/send**  
   **Header:** `Idempotency-Key` (required). Body: payeeBankCode, payeeAccountId, payeeAccountName, amount, remark.  
   **TransactionFacade.applyTransfer:** If transaction with same idempotencyKey exists, returns it. Otherwise calls Choice `trans/v2/applyForTransfer` with payerAccountId = wallet’s choiceAccountId, then creates local `transaction` (externalId = new UUID, status PENDING, type TRANSFER) and saves. Response returns this transaction’s **externalId** (this is the “transactionId” used in subsequent OTP and status calls).

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

2. **Callbacks:** 0002 updates transaction status; 0003 updates wallet balance.

---

## 6. Transaction List and History

- **GET /api/v1/transactions?page=&size=&status=&type=**  
  Local list from `transaction` table for customer (optional filters).

- **GET /api/v1/transactions/choice-history?startTime=&endTime=&page=&size=**  
  Calls Choice Bank API for transaction list (Choice-side).

- **GET /api/v1/transactions/bank-codes**  
  Returns bank codes from Choice for “send money” UI.

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
| 0002 | TransactionResultHandler | Update transaction status, error, completedAt |
| 0003 | BalanceChangeHandler | Update wallet balance_cache, last_balance_update_at |
| UNKNOWN / other | UnknownNotificationHandler | Log only |

All callbacks are persisted in `choice_bank_callback` (raw payload, processed flag). Processing is async; HTTP response is 200 "ok" quickly.
