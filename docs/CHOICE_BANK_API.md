# Choice Bank API Reference

## Base URL

- **Sandbox**: `https://baas-pilot.choicebankapi.com/`
- **Production**: From account manager

## Authentication

- SHA-256 signature with private key
- Request: requestId, sender, locale (en_KE), timestamp, salt, signature, params
- Response: code "00000" = success

## Key APIs

| Endpoint | Purpose |
|----------|---------|
| POST /onboarding/v3/submitEasyOnboardingRequest | Wallet onboarding (ID + selfie) |
| POST /common/sendOtp | Send OTP |
| POST /common/confirmOperation | Confirm OTP |
| POST /trans/v2/applyForTransfer | Initiate transfer |
| POST /trans/depositFromMpesa | STK Push deposit |
| POST /query/getTransResult | Query transaction status |

## Callback Notification Types

- **0001**: Personal Onboarding Result
- **0002**: Transaction Result
- **0003**: Balance Change
- **0021**: Account Status Change

Callback response: HTTP 200 with body `"ok"`. Retries up to 5 times.
