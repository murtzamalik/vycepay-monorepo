# VycePay API Contracts

**Mobile integration:** Use the BFF as single entry point. See [MOBILE_API_CONTRACT.md](MOBILE_API_CONTRACT.md) for base URL, Bearer auth, headers, flows, and error codes.

## Base Path

`/api/v1/`

## Error Envelope

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "requestId": "correlation-id",
  "details": {}
}
```

## Pagination

- `page` (0-based)
- `size`
- Response: `totalElements`, `totalPages`, `content`

## Auth Endpoints

- POST /auth/register
- POST /auth/verify-otp
- POST /auth/login

## KYC Endpoints

- GET /kyc/status
- POST /kyc/submit
- POST /kyc/send-otp?onboardingRequestId=
- POST /kyc/resend-otp?onboardingRequestId=&otpType=SMS
- POST /kyc/confirm-otp (X-Customer-Id header, ?onboardingRequestId=&otpCode=)

## Wallet Endpoints

- GET /wallets/me

## Transaction Endpoints

- POST /transactions/send (Idempotency-Key header)
- POST /transactions/deposit/mpesa (optional header: Idempotency-Key for idempotent deposit)
- POST /transactions/send-otp?transactionId=&otpType=SMS
- POST /transactions/resend-otp?transactionId=&otpType=SMS
- POST /transactions/confirm-otp?transactionId=&otpCode=
- GET /transactions/{transactionId}/status
- GET /transactions/bank-codes (Choice Bank bank codes for transfer UI)
- GET /transactions/choice-history?startTime=&endTime=&page=&size= (Choice Bank transaction list)

Note: `transactionId` in transaction endpoints is the `externalId` (UUID) returned by POST /transactions/send, not the Choice Bank txId.
- GET /transactions?page=0&size=20&status=&type= (optional filters: status, type)

## Activity Endpoints

- POST /activity/log
- GET /activity

## Callback Endpoints

- POST /choice-bank/callback (Choice Bank webhook; raw JSON body)

## OpenAPI / Swagger

Each service exposes interactive API docs:

- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- OpenAPI JSON: `http://localhost:{port}/v3/api-docs`
