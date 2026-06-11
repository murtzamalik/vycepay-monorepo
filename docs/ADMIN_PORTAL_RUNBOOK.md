# VycePay Admin Portal Runbook

## Services

- Backend: `vycepay-admin-service` on port `8090`
- Frontend: `vycepay-admin-portal` on port `3000`
- API prefix: `/api/admin/v1`

## Required Environment

Set these before running a production-like environment:

- `ADMIN_JWT_SECRET`: admin-only signing secret, different from `JWT_SECRET`; required in production and must be at least 32 bytes
- `ADMIN_CORS_ORIGINS`: allowed admin portal origins
- `ADMIN_BOOTSTRAP_USERNAME`: optional first super-admin username
- `ADMIN_BOOTSTRAP_EMAIL`: optional first super-admin email
- `ADMIN_BOOTSTRAP_PASSWORD`: optional first super-admin password; remove after first boot
- `ADMIN_API_URL`: internal URL used by the Next.js server proxy
- `NEXT_PUBLIC_ADMIN_API_URL`: browser-visible API URL for local development
- `ADMIN_RATE_LIMIT_LOGIN_LIMIT`, `ADMIN_RATE_LIMIT_RESET_LIMIT`, `ADMIN_RATE_LIMIT_EXPORT_LIMIT`, `ADMIN_RATE_LIMIT_MUTATION_LIMIT`: optional overrides for sensitive admin endpoint rate limits

Local docker-compose uses an explicit `dev-only-*` admin secret fallback. Production compose requires `ADMIN_JWT_SECRET` and the backend rejects development-only admin secrets when `SPRING_PROFILES_ACTIVE=prod`.

## First Admin Bootstrap

The backend creates an initial `SUPER_ADMIN` only when all bootstrap variables are provided and `admin_user` is empty. There are no hardcoded admin credentials.

Example local boot:

```bash
ADMIN_BOOTSTRAP_USERNAME=admin \
ADMIN_BOOTSTRAP_EMAIL=admin@vycepay.local \
ADMIN_BOOTSTRAP_PASSWORD='change-me-immediately' \
docker compose up -d --build
```

After the first login, rotate the password and remove the bootstrap variables.

## MFA

MFA is disabled per admin user until a Base32 TOTP secret is enrolled in `admin_user.mfa_secret` and `mfa_enabled=true`. When enabled, the login flow requires a standards-compatible 6-digit TOTP code from an authenticator app. There is no hardcoded bypass code.

## Password Reset

Forgot-password requests persist only a SHA-256 hash of the reset token. The API response never returns the raw token, and the log-safe fallback does not print it. Configure a real email/SMS delivery adapter before enabling self-service resets in production; without one, operations must use supervised admin password reset from the portal or database-controlled break-glass procedures.

## Local Validation

```bash
mvn -pl vycepay-admin-service -am test
cd vycepay-admin-portal
npm ci
npm run lint
npm run typecheck
npm test
npm run build
npm run audit:prod
docker compose -f docker-compose.yml config >/dev/null
ADMIN_JWT_SECRET='<prod-like-secret>' JWT_SECRET='<prod-like-secret>' docker compose -f docker-compose.vycepay.yml config >/dev/null
```

## Security Checks

- Verify admin JWTs cannot authenticate customer/mobile APIs.
- Verify customer JWTs cannot authenticate `/api/admin/v1/**`.
- Verify every mutation writes to `admin_audit_log`.
- Verify PII is masked unless the caller has `customer:view_pii`.
- Verify exports are permissioned and audited.
- Verify customer exports require `customer:export` and mask email/mobile unless the caller also has `customer:view_pii`.
- Verify login, reset, export, and mutation rate limits are active.
- Verify sessions are revoked on logout and password reset.
- Verify no reset tokens, JWTs, passwords, or raw PII are present in application logs.

## Operational Alerts

Create alerts for:

- Repeated failed admin logins
- Locked admin accounts
- Permission denied spikes
- Bulk export activity
- Callback retry spikes
- Admin user, role, or permission changes

## Rollback

- Backend rollback: deploy previous `vycepay-admin-service` image.
- Frontend rollback: deploy previous `vycepay-admin-portal` image.
- Database rollback: do not drop admin audit tables in production. Disable new UI/API paths and preserve audit records for investigation.
