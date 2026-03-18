# Choice Bank Setup Guide

## Prerequisites (from Choice Bank)

Contact your Choice Bank account manager to obtain:

| Item | Description | Env Variable |
|------|-------------|--------------|
| **Sender ID** | Unique identifier for your BaaS channel | `CHOICE_BANK_SENDER_ID` |
| **Private Key** | Used to sign requests and verify responses/callbacks | `CHOICE_BANK_PRIVATE_KEY` |
| **Base URL** | Sandbox: `https://baas-pilot.choicebankapi.com` | `CHOICE_BANK_BASE_URL` (optional) |
| **Callback URL** | Your webhook URL (e.g. `https://yourdomain.com/api/v1/choice-bank/callback`) | Registered with account manager |

## Configuration

### KYC & Transaction services

```yaml
vycepay:
  choice-bank:
    base-url: ${CHOICE_BANK_BASE_URL:https://baas-pilot.choicebankapi.com}
    sender-id: ${CHOICE_BANK_SENDER_ID}
    private-key: ${CHOICE_BANK_PRIVATE_KEY}
```

Or via environment variables:
- `CHOICE_BANK_SENDER_ID`
- `CHOICE_BANK_PRIVATE_KEY`
- `CHOICE_BANK_BASE_URL` (optional; defaults to sandbox)

### Callback service (signature verification)

When `vycepay.callback.verify-signature=true`, the callback service verifies webhook signatures using the same private key:

```yaml
vycepay:
  callback:
    verify-signature: ${CALLBACK_VERIFY_SIGNATURE:true}
  choice-bank:
    private-key: ${CHOICE_BANK_PRIVATE_KEY}
```

### Response signature verification (optional)

To verify Choice Bank API response signatures (KYC, Transaction services):

```yaml
vycepay:
  choice-bank:
    verify-response-signature: true
```

## Callback URL registration

1. Provide your webhook URL to your account manager: `https://yourdomain.com/api/v1/choice-bank/callback`
2. Callback activation typically takes 8–10 hours
3. The endpoint must return HTTP 200 with body `"ok"` to acknowledge receipt

## Production

For production, request production Sender ID and Private Key from your account manager. Production base URL will also be provided.
