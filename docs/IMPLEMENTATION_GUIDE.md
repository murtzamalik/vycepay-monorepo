# VycePay Implementation Guide

## Implementation Order

1. vycepay-common - Choice client, signing, DTOs
2. vycepay-database - Flyway migrations
3. vycepay-callback-service - Webhook + routing
4. vycepay-auth-service - Registration, OTP, JWT
5. vycepay-kyc-service - Onboarding, KYC status
6. vycepay-wallet-service - Account mapping, balance cache
7. vycepay-transaction-service - Transfer, deposit, history
8. vycepay-activity-service - Audit logging

## Package Layout (Per Service)

```
com.vycepay.{service}/
├── api/v1/
├── application/service/
├── domain/model/
├── domain/port/
├── infrastructure/persistence/
├── infrastructure/client/
└── config/
```

## Code Standards

- Javadoc on all public classes and methods
- Use Strategy, Adapter, Facade patterns
- See `.cursor/rules/vycepay-architecture.mdc`

## Local Development

1. Start MySQL: `make mysql` or `docker compose up -d`
2. Build: `make build` or `mvn clean compile -DskipTests`
3. Run services (separate terminals): `make run-auth`, `make run-kyc`, etc.
4. Swagger UI per service: http://localhost:{port}/swagger-ui.html

| Service     | Port |
|-------------|------|
| Auth        | 8082 |
| Callback    | 8081 |
| KYC         | 8083 |
| Wallet      | 8084 |
| Transaction | 8085 |
| Activity    | 8086 |
