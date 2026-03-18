# VycePay — Application Purpose and Scope

## Purpose

VycePay is a **white-label digital wallet platform** that:

1. Exposes a **mobile-first API** (via a BFF) for registration, login, KYC, wallet, and transactions.
2. Uses **Choice Bank (Kenya)** as the BaaS provider for:
   - Customer onboarding (KYC) and account opening
   - Transfers (e.g. to M-PESA or bank accounts)
   - M-PESA STK Push deposits
   - Balance and transaction status (via API and callbacks)
3. Maintains **local state** (customer, KYC status, wallet mapping, transaction records, audit log) and keeps **balance cache** and **transaction status** in sync with Choice Bank via **webhooks (callbacks)**.

The application does **not** hold funds; it orchestrates flows and stores references and cached data. Money movement is performed by Choice Bank.

---

## Users

- **End user (mobile app):** Registers with mobile number, completes KYC, receives a wallet (Choice account), sends money, deposits via M-PESA, views balance and history.
- **System/Choice Bank:** Sends callbacks to VycePay to notify onboarding result, transaction result, and balance changes.

---

## In-Scope Features

| Feature | Description |
|--------|-------------|
| Registration & login | OTP to mobile; verify OTP; issue JWT. Customer created on first successful verification. |
| KYC onboarding | Submit onboarding to Choice Bank; send/confirm OTP; poll status; wallet created when Choice notifies account opened (callback 0001). |
| Wallet | Get wallet for current user (balance, choiceAccountId). Balance updated by callback 0003. |
| Send money | Initiate transfer (Choice); optional OTP flow (send-otp, confirm-otp). Status updated by callback 0002. |
| M-PESA deposit | STK Push deposit; optional idempotency. Result/balance via callbacks. |
| Transaction history | List transactions (local); Choice history endpoint for Choice-side list. |
| Bank codes | List bank codes for “send money” UI (Choice API). |
| Activity audit | Log actions (login, send, etc.) for compliance. |
| Callback handling | Receive Choice Bank webhooks; persist; route by notification type; update KYC, wallet, transaction, balance. |

---

## Out of Scope (Current)

- **WebSocket / push:** Real-time balance or transaction updates; mobile polls.
- **White-label branding:** No multi-tenant branding in code; single tenant.
- **Card/other payment rails:** Only Choice Bank BaaS and M-PESA deposit as implemented.
- **Admin UI:** No admin dashboard; APIs only (and Swagger per service).
- **SMS gateway:** OTP “send” is implemented but default is log-only; production would plug a real SMS provider.

---

## Success Criteria (Product)

- Mobile app can register, log in, complete KYC, and see a wallet.
- User can send money (with OTP when required) and deposit via M-PESA.
- Balance and transaction status stay consistent with Choice Bank via callbacks.
- All API access for mobile goes through one entry point (BFF) with JWT.
