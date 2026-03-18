# VycePay Application — Overview (for AI Assistants)

This document is the **entry point** for understanding the VycePay codebase. Use it when working with Claude or any AI on this project.

---

## 1. What is VycePay?

**VycePay** is a **white-label digital wallet platform** that uses **Choice Bank (Kenya)** as its BaaS (Banking-as-a-Service) provider. End users (mobile app) register, complete KYC via Choice Bank’s onboarding, and then get a wallet (Choice account). They can send money (transfers), deposit via M-PESA STK Push, and view balance and transaction history. All banking operations (account creation, transfers, deposits, balance) are performed by Choice Bank APIs; VycePay orchestrates flows, stores mappings and cache, and receives asynchronous results via webhooks (callbacks).

---

## 2. Document Map

| Document | Purpose |
|----------|---------|
| **VYCEPAY_OVERVIEW.md** (this file) | Entry point; purpose; document map; quick architecture and flow summary |
| **APPLICATION_PURPOSE_AND_SCOPE.md** | Product scope, users, features, non-goals |
| **ARCHITECTURE_DETAILED.md** | Services, ports, layers, patterns, deployment, BFF role |
| **BUSINESS_LOGIC_AND_FLOWS.md** | Step-by-step business flows: auth, KYC, wallet, transactions, callbacks |
| **COMMUNICATION_AND_INTEGRATION.md** | How components talk: BFF↔backends, mobile↔BFF, Choice Bank API, webhooks, DB |
| **CHOICE_BANK_INTEGRATION.md** | How Choice Bank is integrated: signing, requestId, outbound APIs, callbacks, config, code components |
| **TECHNICAL_REFERENCE.md** | Ports, config, key classes, DB schema, API base paths, error codes |
| **MOBILE_API_CONTRACT.md** | Mobile-facing API: base URL, auth, headers, flows, error codes |
| **MOBILE_API_CONTRACT_DETAILED.md** | Full mobile contract: request/response shapes for every endpoint |
| **API_CONTRACTS.md** | Backend API endpoints and error envelope |
| **DATABASE_SCHEMA.md** | Tables and relationships |
| **ARCHITECTURE.md** | Short architecture summary (legacy; see ARCHITECTURE_DETAILED for full) |

---

## 3. High-Level Architecture

- **Single entry for mobile:** **BFF** (Backend-for-Frontend) on port **8080**. Mobile app calls only the BFF; BFF validates JWT and proxies to backend services by path prefix.
- **Backend services** (each a separate Spring Boot app, same repo):
  - **Auth** (8082): Registration, OTP, login, JWT.
  - **KYC** (8083): Onboarding submit, OTP send/confirm, status; talks to Choice Bank.
  - **Wallet** (8084): Wallet by customer; balance from DB (updated by callbacks).
  - **Transaction** (8085): Send money, M-PESA deposit, OTP for transfer, status, history; talks to Choice Bank.
  - **Activity** (8086): Audit log (login, send, etc.).
  - **Callback** (8081): Single webhook for Choice Bank; persists and routes by `notificationType` (0001, 0002, 0003, etc.).
- **Shared MySQL** database; all services use the same schema (Flyway in `vycepay-database`).
- **Choice Bank** is the external banking provider: onboarding, transfers, M-PESA deposit, balance/transaction updates arrive asynchronously via callbacks.

---

## 4. Business Flow Summary

1. **Auth:** Register (send OTP) → verify OTP → get JWT. Login = send OTP → verify OTP → get JWT. Customer created on first successful verify.
2. **KYC:** Get status → submit onboarding (Choice) → send OTP → confirm OTP. Callback **0001** updates KYC and creates **wallet** when status = account opened.
3. **Wallet:** GET wallet/me returns balance and `choiceAccountId` (404 until wallet exists after KYC).
4. **Send money:** POST send with **Idempotency-Key**; if Choice requires OTP: send-otp → confirm-otp. Callback **0002** updates transaction status.
5. **Deposit (M-PESA):** POST deposit/mpesa (optional Idempotency-Key). Callbacks **0002** and **0003** update tx and balance.
6. **Callbacks:** Choice Bank POSTs to `/api/v1/choice-bank/callback`. Stored in `choice_bank_callback`; routed by `notificationType` (0001=onboarding, 0002=transaction, 0003=balance). Processing is async; response is always 200 "ok".

---

## 5. Communication Summary

- **Mobile → BFF:** REST, `Authorization: Bearer <JWT>`. BFF sets `X-Customer-Id` (customer’s `externalId`) when proxying.
- **BFF → Backends:** HTTP to `auth`, `kyc`, `wallets`, `transactions`, `activity` by first path segment under `/api/v1/`.
- **Backends → Choice Bank:** HTTPS, signed requests (see `vycepay-common`: `ChoiceBankClient`, `ChoiceBankRequestFactory`, BaaS signature in body).
- **Choice Bank → VycePay:** Webhook POST to callback service; body JSON with `requestId`, `notificationType`, `params`.

---

## 6. Key Conventions for Development

- **Customer identity:** APIs use **externalId** (UUID) from `customer` table; JWT contains it; BFF passes it as `X-Customer-Id`.
- **Transaction id in APIs:** “transactionId” in transaction endpoints is the **externalId** (UUID) of the `transaction` row (from POST send response), not Choice’s txId.
- **Idempotency:** POST send requires `Idempotency-Key`; POST deposit/mpesa supports optional `Idempotency-Key`.
- **Real-time:** No WebSockets yet; mobile polls wallet and transaction status.

---

## 7. How to Use These Docs with Claude

- Start with **VYCEPAY_OVERVIEW.md** (this file) for context.
- For “how does X work?” use **BUSINESS_LOGIC_AND_FLOWS.md** and **COMMUNICATION_AND_INTEGRATION.md**.
- For “where is X implemented?” use **ARCHITECTURE_DETAILED.md** and **TECHNICAL_REFERENCE.md** (and grep by service/module).
- For mobile integration use **MOBILE_API_CONTRACT.md** and **API_CONTRACTS.md**.
- For schema and tables use **DATABASE_SCHEMA.md** and **TECHNICAL_REFERENCE.md**.

All documents live under the **vycepay** project folder, in the **docs/** directory.
