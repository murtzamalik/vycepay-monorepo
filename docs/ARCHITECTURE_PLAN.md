# VycePay Full Architecture Plan

Complete reference. See [ARCHITECTURE.md](ARCHITECTURE.md) for summary.

## Choice Bank API Behavior

- **Auth**: SHA-256 signature, requestId, sender, locale, timestamp, salt, params
- **Callbacks**: Return HTTP 200 + "ok"; retries up to 5 times
- **Async**: Onboarding and transfers complete via callbacks (0001, 0002, 0003)
- **Transaction status**: -1 Timeout, 1 Pending, 2 Processing, 4 Failed, 8 Success

## Service Implementation Status

1. vycepay-common - Done
2. vycepay-database - Done
3. vycepay-callback-service - Done
4. vycepay-auth-service - Done (register, verify-otp, login, JWT)
5. vycepay-kyc-service - Done (status, submit, send-otp, confirm-otp)
6. vycepay-wallet-service - Done (GET /wallets/me)
7. vycepay-transaction-service - Done (send, deposit/mpesa, list)
8. vycepay-activity-service - Done (log, list)

## Design Patterns

- Strategy: NotificationHandler per callback type
- Adapter: ChoiceBankApiAdapter implements BankingProviderPort for Choice API
- Factory: ChoiceBankRequestFactory for signed requests
- Facade: CallbackService orchestrates persist + route

## Recent Additions

- **Resilience4j**: Retry and circuit breaker on Choice Bank client (KYC, Transaction services)
- **OpenAPI/Swagger**: Interactive API docs at `/swagger-ui.html` per service
- **Transaction OTP**: send-otp, confirm-otp for Choice transfers
- **Transaction status**: GET /transactions/{id}/status (Choice getTransResult)

## Gap Closures (Completed)

- **BusinessException adoption**: All facades and controllers use BusinessException with appropriate codes (CUSTOMER_NOT_FOUND, TRANSACTION_NOT_FOUND, ONBOARDING_FAILED, etc.)
- **ResponseSignatureVerifier integration**: ChoiceBankApiAdapter verifies responses when verifier is configured (`vycepay.choice-bank.verify-response-signature=true`)
- **SensitiveDataEncryptionPort usage**: OnboardingResultHandler encrypts `id_number` before storing in kyc_verification
- **Resilience4j exponential backoff**: Retry config uses `enableExponentialBackoff: true`, `exponentialBackoffMultiplier: 2`

## Phase 1 Completion (Rule Book Alignment)

- **BusinessException**: Domain exception with code, message, HTTP status; handled by GlobalExceptionHandler
- **BankingProviderPort / ChoiceBankApiAdapter**: Hexagonal adapter; facades depend on port
- **@Schema on DTOs**: OpenAPI annotations on all API request/response DTOs
- **SensitiveDataEncryptionPort**: Conceptual encryption hook; NoOp impl for dev, pluggable for prod
- **ResponseSignatureVerifier**: Choice Bank response signature verification (enable via `vycepay.choice-bank.verify-response-signature=true`)
- **JPA Specification**: TransactionSpecification for status/type/date/amount filters; GET /transactions?status=&type=
- **Actuator**: Health, info, metrics on all services; see docs/ACTUATOR.md
