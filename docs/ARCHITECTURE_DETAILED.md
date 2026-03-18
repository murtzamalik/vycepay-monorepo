# VycePay — Detailed Architecture

## 1. Overview

- **Multi-service:** Several Spring Boot applications in one repo (Maven multi-module).
- **Single DB:** One MySQL database shared by all services; schema via Flyway in `vycepay-database`.
- **Single entry for mobile:** BFF (Backend-for-Frontend) on port 8080; validates JWT and proxies to backend services.
- **External banking:** Choice Bank BaaS (Kenya); outbound API calls and inbound webhooks (callbacks).

---

## 2. Module and Service Map

| Module | Port | Responsibility |
|--------|------|----------------|
| **vycepay-bff** | 8080 | Single entry for mobile; JWT validation; proxy to auth, kyc, wallets, transactions, activity. Swagger enabled. |
| **vycepay-auth-service** | 8082 | Registration (send OTP), verify OTP, login; JWT issuance; customer create/lookup. |
| **vycepay-kyc-service** | 8083 | KYC status; submit onboarding to Choice; send/resend/confirm OTP (Choice); get onboarding status. |
| **vycepay-wallet-service** | 8084 | Get wallet by customer (balance, choiceAccountId); data from DB (balance updated by callbacks). |
| **vycepay-transaction-service** | 8085 | Send money, M-PESA deposit, send/resend/confirm OTP for transfer; tx status; list; bank codes; Choice history. |
| **vycepay-activity-service** | 8086 | Audit log (POST log, GET activity). |
| **vycepay-callback-service** | 8081 | Choice Bank webhook receiver; persist; route by notificationType; async processing. |
| **vycepay-common** | — | Choice Bank client, request factory, signing, DTOs, JWT validator, shared exceptions. |
| **vycepay-database** | — | Flyway migrations only. |

---

## 3. Per-Service Layered Structure

Each service follows:

```
api/           → REST controllers, request/response DTOs
application/   → Use cases, facades, application services
domain/        → Entities, value objects, domain ports
infrastructure/→ JPA repositories, external clients (e.g. Choice Bank adapter)
config/        → Spring configuration (security, beans)
```

---

## 4. Design Patterns

| Pattern | Where |
|---------|--------|
| **Strategy** | Callback: `NotificationHandler` per `notificationType` (0001, 0002, 0003, UNKNOWN). |
| **Adapter** | `ChoiceBankApiAdapter` implements `BankingProviderPort`; used by KYC and Transaction services. |
| **Factory** | `ChoiceBankRequestFactory` builds signed request body (requestId, timestamp, salt, signature, params). |
| **Facade** | `AuthFacade`, `KycOnboardingFacade`, `TransactionFacade` orchestrate use cases. |
| **Repository** | Spring Data JPA repositories per aggregate. |
| **Circuit Breaker / Retry** | Resilience4j on Choice Bank HTTP calls (in common/client). |

---

## 5. BFF (Backend-for-Frontend)

- **Role:** Single HTTP entry point for the mobile app.
- **Security:** `BffJwtFilter` validates `Authorization: Bearer <token>`, extracts customer `externalId`, sets request attribute `X-Customer-Id`. Public paths: `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/verify-otp`; plus `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`.
- **Proxy:** `BffProxyController` matches `/api/v1/**`, takes first path segment (`auth`, `kyc`, `wallets`, `transactions`, `activity`), forwards request to corresponding backend base URL with same path and query; injects `X-Customer-Id`; returns backend response or 502/404 on failure.
- **Config:** `BffBackendProperties` (e.g. `vycepay.bff.auth-url`, `kyc-url`, …). In Docker, backends are `http://127.0.0.1:8082`, etc.

---

## 6. Callback Service (Webhook)

- **Endpoint:** `POST /api/v1/choice-bank/callback` (raw JSON body). Not exposed to mobile; called by Choice Bank.
- **Flow:** Optional signature verification → persist in `choice_bank_callback` (idempotent by requestId + notificationType) → return 200 "ok" → process **asynchronously** via `CallbackService.processAsync`.
- **Routing:** By `notificationType` from payload:
  - **0001** — Onboarding result: `OnboardingResultHandler` (update `kyc_verification`; create `wallet` when status = 7).
  - **0002** — Transaction result: `TransactionResultHandler` (update `transaction` status, error, completedAt).
  - **0003** — Balance change: `BalanceChangeHandler` (update `wallet.balance_cache`, `last_balance_update_at`).
  - **UNKNOWN** — `UnknownNotificationHandler` (log only).

---

## 7. Ports and Adapters (Hexagonal)

- **Primary (inbound):** BFF REST, backend REST controllers, callback webhook.
- **Secondary (outbound):** `BankingProviderPort` → `ChoiceBankApiAdapter` (Choice Bank HTTP); JPA repositories → MySQL.

---

## 8. Deployment

- **Local:** Run each service with `mvn spring-boot:run -pl <module>`; same MySQL; configure DB and (for Choice) base URL, senderId, privateKey.
- **Docker (single container):** `Dockerfile.vycepay` builds all JARs; `docker-entrypoint.sh` starts MariaDB, then auth, callback, kyc, wallet, transaction, activity, then BFF. BFF exposed on host port 9080 (container 8080). Compose: `docker-compose.vycepay.yml`.

---

## 9. Real-Time and Future

- **Current:** Balance and transaction status are obtained by **polling** (e.g. GET wallet/me, GET transaction status).
- **Future:** WebSocket or push (e.g. FCM/APNs) when callbacks update state is TBD; contract and implementation not yet defined.
