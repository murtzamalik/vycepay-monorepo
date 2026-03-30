# VycePay — Choice Bank Integration

This document describes **how VycePay integrates with Choice Bank**: authentication, outbound API usage, inbound callbacks, configuration, and the code that implements it.

---

## 1. Overview

- **Choice Bank** provides BaaS (Banking-as-a-Service) in Kenya. VycePay uses their **BaaS API** for:
  - Customer onboarding (KYC) and account opening
  - Account management (details, short codes, dormant activation, email/mobile verification, SME sub-account name)
  - Periodic account statements (apply/query; file ready via callback **0009**)
  - Transfers (e.g. to M-PESA or bank)
  - Utility payments (airtime, bill query/pay, payment queries)
  - M-PESA STK Push deposits
  - Bank codes, transaction status, transaction list
- **Authentication:** Every outbound request is **signed**; the signature is sent **inside the JSON request body**. Choice validates it and returns `code: "00000"` on success.
- **Asynchronous results:** Onboarding completion, transaction result, balance changes, statement file readiness, and account status changes are notified via **HTTP callbacks** (webhooks) to VycePay. VycePay stores and processes them by `notificationType` (0001, 0002, 0003, 0009, 0021).

---

## 2. Outbound API: Request Format and Signing

### 2.1 Request envelope

Every POST to Choice Bank uses the same envelope (see `ChoiceBankRequest` in `vycepay-common`):

| Field       | Description |
|------------|-------------|
| `requestId` | Unique per request; format **senderId + 32 hex chars** (e.g. `VYCEIN` + UUID without hyphens). |
| `sender`    | Sender ID (e.g. `VYCEIN`). Must match the prefix in `requestId`. |
| `locale`    | Fixed `en_KE`. |
| `timestamp` | Current time in **milliseconds** (13 digits). |
| `salt`      | Random 12-character string (e.g. from UUID). |
| `signature` | SHA-256 **hex** of the string-to-sign (see below). **senderKey is not sent** in the body; it is only used when computing the signature. |
| `params`    | JSON object with endpoint-specific parameters (e.g. onboarding data, transfer details). |

**Important:** `params` may be empty `{}`. For signing, the flattened string must still include `params={}` (see below).

### 2.2 String-to-sign (BaaS)

Signature is computed as follows (implemented in `ChoiceBankRequestFactory` and `ChoiceBankSignatureUtil`):

1. **Flatten** the following into key–value pairs (nested keys use dot notation, e.g. `params.payeeBankCode`):
   - `locale`, `requestId`, `salt`, `sender`, `timestamp`
   - `params` — either flattened recursively as `params.key1`, `params.key2`, … or, if empty, the literal **`params={}`**
2. **Add** `senderKey` = private key **only for signing**; do **not** put `senderKey` in the request body.
3. **Sort** all keys in **ASCII** order (e.g. `TreeMap` with `String::compareTo`).
4. **Build** the string: `key1=value1&key2=value2&...` (no leading or trailing spaces).
5. **Hash:** SHA-256 of this string, output as **hex** (lowercase). This is the `signature` value.

Example (conceptual):  
`locale=en_KE&params={}&requestId=VYCEIN...&salt=abc123&sender=VYCEIN&senderKey=<private_key>&timestamp=1234567890123`

### 2.3 requestId format

- **Required:** `requestId = senderId + 32 hex characters` (UUID without hyphens).
- **Wrong:** `VYCE-` + UUID with hyphens, or any format that does not match this.
- **Implementation:** `RequestIdGenerator.generate(senderId)` in `vycepay-common`. Used by `ChoiceBankClient` when building each request.

---

## 3. Outbound API: Endpoints Used by VycePay

Base URL is configurable (e.g. sandbox `https://baas-pilot.choicebankapi.com`). Paths are relative to that base.

| Path | Used by | Purpose |
|------|--------|---------|
| `onboarding/v3/submitEasyOnboardingRequest` | KYC service | Submit KYC onboarding; returns onboardingRequestId. |
| `onboarding/getOnboardingStatus` | KYC service | Poll onboarding status (onboardingRequestId). |
| `common/sendOtp` | KYC, Transaction | Send OTP (businessId = onboardingRequestId or transaction id, otpType = SMS/EMAIL). |
| `common/resendOtp` | KYC, Transaction | Resend OTP. |
| `common/confirmOperation` | KYC, Transaction | Confirm OTP (businessId, otpCode). |
| `trans/v2/applyForTransfer` | Transaction service | Initiate transfer (payerAccountId, payeeBankCode, payeeAccountId, amount, currency, etc.). |
| `trans/depositFromMpesa` | Transaction service | M-PESA STK Push deposit (accountId, mobile, amount). |
| `query/getTransResult` | Transaction service | Get transaction result (txId or requestId). |
| `query/getTransList` | Transaction service | Get transaction list from Choice (time range, pagination). |
| `staticData/getBankCodes` | Transaction service | Get bank codes for “send money” UI. |
| `query/getAccountDetails`, `account/queryAccountListByUserId`, `query/getAbnormalAccountList`, `account/applyForShortCode`, `account/queryForShortCode`, `account/queryAccountByShortCode`, `account/activateAccount`, `user/addOrUpdateEmail`, `account/v2/mobileChange`, `account/confirmMobileChange`, `account/verifyEmailAddress`, `account/verifyEmailOrMobile`, `account/editSubAccountName`, `account/verifyOtp` | Wallet service | Account lifecycle and contact verification (`verifyOtp` is **not** `common/confirmOperation`). |
| `statement/applyAccountStatement`, `statement/queryAccountStatement` | Wallet service | Periodic statement; local row in `account_statement_job`. |
| `utilityPayment/v2/airtimePayment`, `utilityPayment/v2/airtimeBulkPayment`, `utilityPayment/billQuery`, `utilityPayment/v2/billPayment`, `utilityPayment/paymentQuery`, `utilityPayment/bulkPaymentQuery` | Transaction service | Utilities; debits create `transaction` rows (`UTILITY_*` types). |

All calls go through the **same** client and signing logic; only `params` and path change.

---

## 4. Response Handling

- **Success:** Choice returns `code: "00000"`. Payload is in `data` (structure depends on endpoint).
- **Errors:** Non-00000 codes (e.g. 12004 “Invalid signature”, 12002 “Invalid request id”). VycePay maps these to BAD_GATEWAY or passes through; mobile sees message/code in error envelope.
- **requestId:** Response may include `requestId`; VycePay stores it on the transaction (e.g. `choiceRequestId`) where relevant.

---

## 5. Inbound Callbacks (Webhooks)

### 5.1 Endpoint and contract

- **URL:** VycePay exposes `POST /api/v1/choice-bank/callback` on the **callback service** (port 8081). This URL must be registered with Choice Bank and reachable from the internet in production.
- **Body:** Raw JSON. Top-level fields include:
  - `requestId`
  - `notificationType` — determines which handler runs (e.g. 0001, 0002, 0003).
  - `params` — nested object with type-specific payload.
- **Response:** HTTP **200** with body `"ok"` so Choice considers delivery successful. Processing is **asynchronous** after persisting the payload.

### 5.2 Notification types and handlers

| notificationType | Handler | Action |
|------------------|---------|--------|
| **0001** | OnboardingResultHandler | Update `kyc_verification` (status, userId, accountId, rejection info). If status = 7 (account opened), create `wallet` for that customer. |
| **0002** | TransactionResultHandler | Find transaction by choiceTxId or choiceRequestId; update status, errorCode, errorMsg, completedAt. |
| **0003** | BalanceChangeHandler | Find wallet by accountId; update balance_cache and last_balance_update_at. |
| **0009** | AccountStatementResultHandler | Find `account_statement_job` by `requestId` (or equivalent in `params`); set `download_url` / status when statement file is ready. Confirm field names with Choice. |
| **0021** | AccountStatusChangeHandler | Find wallet by `accountId`; update `wallet.status` from Choice account status fields. |
| Other | UnknownNotificationHandler | Log only. |

### 5.3 Callback flow in code

1. **CallbackService.receiveAndProcess:** Optionally verify signature (if enabled), then persist raw body in `choice_bank_callback` (idempotent by requestId + notificationType).
2. Return 200 "ok" to Choice.
3. **processAsync:** Load callback record, select handler by `notificationType`, run handler, set `processed=true`.

Duplicate callbacks (same requestId + notificationType) are stored once and processed once.

---

## 6. Configuration

Choice Bank integration is **optional** per service: KYC, Transaction, and Wallet services create the Choice client and adapter when the following are set.

| Config key (env or application.yml) | Purpose |
|-------------------------------------|---------|
| `vycepay.choice-bank.base-url` | Base URL (e.g. `https://baas-pilot.choicebankapi.com`). Default in code: same. |
| `vycepay.choice-bank.sender-id` | Sender ID (e.g. `VYCEIN`). Required for client to be created. |
| `vycepay.choice-bank.private-key` | Private key used for BaaS request signing. |

Environment variables (often used in Docker) may be mapped to these (e.g. `CHOICE_BANK_BASE_URL`, `CHOICE_BANK_SENDER_ID`, `CHOICE_BANK_PRIVATE_KEY`). Check each service’s `application.yml` for `spring.config.import` or direct property names.

**Callback service:** Optional `vycepay.callback.verify-signature` to verify incoming callback signatures (if Choice supports it).

### 6.1 Outbound request/response logging (BaaS client)

`ChoiceBankClient` logs each outbound call at **INFO** with the same **`choiceBankRequestId`** (the per-request `requestId`) on the request and response lines so logs can be correlated:

- `choice_baas_request` — path and JSON payload (optional).
- `choice_baas_response` — path, `code`, `msg`, and raw response JSON (optional).

| Config key | Default | Purpose |
|------------|---------|---------|
| `vycepay.choice-bank.logging.enabled` | `true` | Master switch for these logs. |
| `vycepay.choice-bank.logging.log-bodies` | `true` | If `false`, only path, `requestId`, and response `code`/`msg` are logged (no JSON bodies). |
| `vycepay.choice-bank.logging.redact-signatures` | `true` | If `true`, `signature` and `salt` fields in logged JSON are replaced with `[REDACTED]` (including nested objects). |

**Note:** `params` may contain PII (e.g. mobile numbers). Restrict log access in production; set `log-bodies` to `false` if you only need metadata.

---

## 7. Code Components (Where It Lives)

| Component | Module | Role |
|-----------|--------|------|
| **BankingProviderPort** | vycepay-common | Interface: `post(path, params)` → ChoiceBankResponse. |
| **ChoiceBankApiAdapter** | vycepay-common | Implements BankingProviderPort; delegates to ChoiceBankClient; optional response signature verification. |
| **ChoiceBankClient** | vycepay-common | Builds request (via ChoiceBankRequestFactory), sends HTTP POST, parses ChoiceBankResponse. Uses RequestIdGenerator.generate(senderId). Logs paired request/response (`choice_baas_*`) when `vycepay.choice-bank.logging.enabled` is true. |
| **ChoiceBankRequestFactory** | vycepay-common | Builds ChoiceBankRequest: requestId, sender, locale, timestamp, salt, **signature** (via ChoiceBankSignatureUtil.sign), params. Flattens for signing; empty params → `params={}`. |
| **ChoiceBankSignatureUtil** | vycepay-common | BaaS: buildStringToSign (sort keys, key=value&…), sha256Hex, sign(flatMap). Also has white-label HMAC helpers (not used for current BaaS flow). |
| **RequestIdGenerator** | vycepay-common | Generates requestId = senderId + UUID without hyphens. |
| **ChoiceBankClientConfig** | vycepay-common | Spring bean for ChoiceBankClient when sender-id (and private-key) are set; wires Resilience4j retry and circuit breaker if present. |
| **KycOnboardingFacade** | vycepay-kyc-service | Uses BankingProviderPort for submitEasyOnboardingRequest, getOnboardingStatus, sendOtp, resendOtp, confirmOperation. |
| **TransactionFacade** | vycepay-transaction-service | Uses BankingProviderPort for applyForTransfer, depositFromMpesa, getTransResult, getTransList, getBankCodes, sendOtp, resendOtp, confirmOperation. |
| **UtilityPaymentFacade** | vycepay-transaction-service | Utility payment and query endpoints; debits persist transactions. |
| **AccountManagementFacade** | vycepay-wallet-service | Choice account management and `account/verifyOtp`. |
| **AccountStatementFacade** | vycepay-wallet-service | `statement/applyAccountStatement`, `statement/queryAccountStatement`; persists `account_statement_job`. |
| **CallbackService** | vycepay-callback-service | Receives POST, persists callback, routes by notificationType to NotificationHandler implementations. |
| **OnboardingResultHandler, TransactionResultHandler, BalanceChangeHandler, AccountStatementResultHandler, AccountStatusChangeHandler** | vycepay-callback-service | Update kyc_verification / wallet / transaction / statement job from callback params. |

---

## 8. Resilience and Errors

- **Retry / circuit breaker:** When Resilience4j is on the classpath, ChoiceBankClientConfig can register a Retry and CircuitBreaker for the Choice client (instance name e.g. `choiceBank`). Failures (network, 5xx) can be retried; repeated failures can open the circuit.
- **Business errors:** Non-00000 responses are surfaced to the caller (KYC or Transaction facade), which may throw BusinessException with BAD_GATEWAY or return error to client.
- **Callback failures:** If handler throws, the callback record is updated with `processingError` and remains `processed=false` for possible manual retry or investigation.

---

## 9. Summary

- **Outbound:** One signed request format (requestId, sender, locale, timestamp, salt, signature, params); requestId = senderId + 32 hex chars; signature = SHA-256 hex of sorted flattened string including `params={}` when empty; senderKey used only for signing, not sent. All outbound calls go through ChoiceBankClient → ChoiceBankRequestFactory → ChoiceBankSignatureUtil.
- **Inbound:** Single webhook URL; persist then 200 "ok"; async dispatch by notificationType to update KYC, wallet, and transaction tables.
- **Config:** base-url, sender-id, private-key enable the client; KYC, Transaction, and Wallet services use BankingProviderPort (ChoiceBankApiAdapter) for Choice Bank operations they expose.

For API endpoint details and callback payload examples, see Choice Bank’s official documentation and, in this repo, [CHOICE_BANK_API.md](CHOICE_BANK_API.md).
