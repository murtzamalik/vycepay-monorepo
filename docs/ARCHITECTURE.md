# VycePay Architecture

## Overview

White-label digital wallet platform using Choice Bank (Kenya) as BaaS provider. Multiple Spring Boot services sharing a single MySQL database, separated by responsibility.

## Layered Architecture (Per Service)

```
api/           → Controllers, request/response DTOs
application/   → Use cases, orchestration, facades
domain/        → Entities, domain services, value objects, ports
infrastructure/→ Repositories, external clients, adapters
config/        → Spring configuration
```

## Design Patterns

| Pattern | Usage |
|---------|-------|
| **Strategy** | Callback routing: `NotificationHandler` per notificationType |
| **Adapter** | `ChoiceBankApiAdapter` implements `BankingProviderPort` |
| **Factory** | `ChoiceBankRequestFactory` for signed requests |
| **Facade** | `KycOnboardingFacade`, `TransactionFacade` |
| **Repository** | Spring Data JPA |
| **Circuit Breaker** | Resilience4j on Choice Bank calls |
| **Retry** | Exponential backoff for transient failures |

## Service Map

| Service | Port | Responsibility |
|---------|------|----------------|
| BFF | 8080 | Single entry for mobile; JWT validation; proxy to auth, KYC, wallet, transaction, activity |
| Auth | 8082 | Registration, OTP, JWT, sessions |
| KYC | 8083 | Profile, onboarding, Choice submit, KYC status |
| Wallet | 8084 | Account mapping, balance cache |
| Transaction | 8085 | Transfer, deposit, idempotency, history |
| Callback | 8081 | Single webhook, persist, route to handlers |
| Activity | 8086 | Audit log (login, send, view) |

## Ports and Adapters

- **Primary (Inbound)**: BFF (mobile), REST controllers, callback webhook
- **Secondary (Outbound)**: `BankingProviderPort` → Choice Bank adapter; JPA repositories

## Future: real-time updates

Balance and transaction status today are obtained by polling: `GET /api/v1/wallets/me` and `GET /api/v1/transactions/{transactionId}/status`. Later we may add WebSocket or push (e.g. FCM/APNs) when callbacks update state; contract and implementation are TBD.
