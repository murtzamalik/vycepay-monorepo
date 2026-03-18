# VycePay Digital Wallet Platform

White-label digital wallet using Choice Bank (Kenya) BaaS APIs.

## Documentation

All documentation is in the [`docs/`](docs/) folder. **For a full picture (including when using Claude):** start with [**docs/VYCEPAY_OVERVIEW.md**](docs/VYCEPAY_OVERVIEW.md).

- [**VYCEPAY_OVERVIEW**](docs/VYCEPAY_OVERVIEW.md) — Entry point: purpose, architecture summary, flows, doc map (for AI and humans)
- [Architecture (detailed)](docs/ARCHITECTURE_DETAILED.md) · [Architecture (short)](docs/ARCHITECTURE.md)
- [Business logic and flows](docs/BUSINESS_LOGIC_AND_FLOWS.md) · [Communication and integration](docs/COMMUNICATION_AND_INTEGRATION.md)
- [Technical reference](docs/TECHNICAL_REFERENCE.md) · [Database Schema](docs/DATABASE_SCHEMA.md)
- [Mobile API contract](docs/MOBILE_API_CONTRACT.md) · [API Contracts](docs/API_CONTRACTS.md)
- [Choice Bank API](docs/CHOICE_BANK_API.md) · [Implementation Guide](docs/IMPLEMENTATION_GUIDE.md)

## Tech Stack

- Java 17, Spring Boot 3.2
- MySQL, Flyway
- Maven

## Modules

| Module | Description | Port |
|--------|-------------|------|
| vycepay-common | Choice Bank client, signing, DTOs | - |
| vycepay-database | Flyway migrations | - |
| vycepay-bff | Single entry for mobile; JWT + proxy to backends | 8080 |
| vycepay-callback-service | Webhook receiver, Strategy handlers | 8081 |
| vycepay-auth-service | Registration, OTP, JWT | 8082 |
| vycepay-kyc-service | Onboarding, Choice submit, OTP confirm | 8083 |
| vycepay-wallet-service | Account mapping, balance cache | 8084 |
| vycepay-transaction-service | Transfer, deposit, history, idempotency | 8085 |
| vycepay-activity-service | Audit logging | 8086 |

## Docker

**Multi-service (one container per app + MySQL):**
```bash
docker compose up -d --build
```
- Uses `docker-compose.yml` and `Dockerfile.services`. BFF: http://localhost:9080. Set `JWT_SECRET`, `CHOICE_BANK_SENDER_ID`, `CHOICE_BANK_PRIVATE_KEY` (and optionally `MYSQL_*`) via `.env` or environment.

**Single container (all apps + MariaDB in one):**
```bash
docker compose -f docker-compose.vycepay.yml up -d --build
```
- Uses `docker-compose.vycepay.yml` and `Dockerfile.vycepay`. BFF: http://localhost:9080.

## Quick Start

1. Create MySQL database: `CREATE DATABASE vycepay;`
2. Configure `application.yml` (or env): `DB_USERNAME`, `DB_PASSWORD`
3. Run: `mvn spring-boot:run -pl vycepay-callback-service`
4. Callback endpoint: `POST http://localhost:8081/api/v1/choice-bank/callback`

## Choice Bank Webhook

Register with Choice: `https://your-domain/api/v1/choice-bank/callback`  
Response must be HTTP 200 with body `"ok"`.
