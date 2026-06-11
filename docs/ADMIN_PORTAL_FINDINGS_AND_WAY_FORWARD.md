# VycePay Admin Portal Findings & Way Forward

## Executive Summary

The admin/backoffice portal is a valid and important next step for VycePay. The design in `admin-portal-design/index.html` provides a strong visual prototype for operations, finance, compliance, system monitoring, and admin management workflows. The technical plan in `docs/ADMIN_PORTAL_TECHNICAL_PLAN.md` also gives a useful implementation direction: create a new `vycepay-admin-service`, a new `vycepay-admin-portal`, and admin-specific RBAC/audit tables.

Before implementation begins, the plan needs production hardening for a fintech backoffice environment. The biggest gaps are action-level RBAC, MFA, immutable audit logging, PII masking, secure admin sessions, export controls, maker-checker controls for risky actions, and formal API contracts.

Recommended direction: implement this as a separate admin backend and separate frontend, backed by the existing shared MySQL database, with strict read/write boundaries and admin-specific security controls.

## Production Hardening Addendum

The current implementation now addresses the highest-risk production blockers: password-reset tokens are hashed and are not returned to the frontend, MFA verification uses standards-compatible TOTP when enabled, logout revokes backend sessions, sensitive endpoints have rate limits, risky mutations use typed validated request DTOs with mandatory reasons, customer exports require `customer:export` and mask PII unless `customer:view_pii` is present, production admin JWT secrets fail closed, and CI validates backend tests, frontend lint/typecheck/test/build/audit, and compose configuration.

Remaining operator-dependent items before production launch: configure a real password-reset delivery provider, enroll MFA secrets for admin users, provision production-grade secrets through the deployment secret manager, and connect operational alerting/SIEM to admin audit and authentication events.

## Scope Reviewed

Reviewed artifacts:

- `admin-portal-design/index.html`
- `docs/ADMIN_PORTAL_TECHNICAL_PLAN.md`
- `README.md`
- `pom.xml`
- `docker-compose.yml`
- `docker-compose.vycepay.yml`
- `Dockerfile.services`
- `vycepay-database/src/main/resources/db/migration/V1__initial_schema.sql`

Relevant existing backend areas:

- `vycepay-auth-service`
- `vycepay-kyc-service`
- `vycepay-wallet-service`
- `vycepay-transaction-service`
- `vycepay-callback-service`
- `vycepay-activity-service`
- `vycepay-bff`
- `vycepay-common`

## Current Understanding

The intended portal covers:

- Admin authentication: login, forgot/reset password
- Dashboard: KPIs, transaction volume, KYC status, alerts, recent transactions
- Customer operations: list, detail, edit/status actions, activity timeline
- KYC operations: list, detail, Choice Bank references, documents/timeline
- Wallet operations: list, detail, freeze/query balance actions
- Transactions: all transactions, failed transactions, detail views
- Callback monitoring: callback log, callback detail, retry action
- Reports: transaction volume, KYC funnel, customer growth
- Audit log: customer/system activity visibility
- System health: internal services and Choice Bank status
- Admin management: menus, roles, admin users

The existing backend already has most operational data tables:

- `customer`
- `kyc_verification`
- `wallet`
- `transaction`
- `choice_bank_callback`
- `activity_log`

The missing domain is admin-specific identity, RBAC, session management, and audit.

## Key Findings

1. The static design is strong but not implementation-ready by itself.
   `admin-portal-design/index.html` is a single HTML prototype using mock data, inline styles, CDN Tailwind, and Chart.js. It should be converted into a real frontend application with typed APIs, reusable components, loading/error states, accessibility, and secure auth.

2. The technical plan has the right broad architecture.
   `docs/ADMIN_PORTAL_TECHNICAL_PLAN.md` correctly proposes a separate `vycepay-admin-service` and `vycepay-admin-portal`.

3. The current repo has no existing web frontend.
   No `package.json`, `next.config.*`, `tailwind.config.*`, or Vite config exists at repo level. The admin portal will be a new frontend application.

4. Existing backend APIs are customer-scoped.
   Most current services expect customer context and are designed for mobile/customer flows. Admin workflows require cross-customer search, reporting, exports, and controlled mutations.

5. Admin security must be stronger than the current plan.
   Menu-based RBAC is not enough. Production backoffice access needs action-level permissions, MFA, short-lived sessions, login throttling, audit logs, export controls, and PII masking.

6. Existing transaction data has an entity gap.
   The database has `error_code` and `error_msg` columns in the `transaction` table, but `vycepay-transaction-service/src/main/java/com/vycepay/transaction/domain/model/Transaction.java` does not currently expose them.

## Gap Analysis

### Product / Requirements

- Define launch roles: Super Admin, Operations, Finance, Compliance, Support, and Read-only Auditor.
- Define exact permissions per role.
- Clarify which actions are allowed:
  - Suspend customer
  - Deactivate customer
  - Freeze wallet
  - Query Choice Bank balance/status
  - Retry callback
  - Export customer/transaction/audit data
  - View KYC documents
  - View raw callback payloads
- Define whether high-risk actions require maker-checker approval.

### Design / UX

- Add loading, empty, error, permission-denied, and partial-data states.
- Add confirmation modals with reason capture for risky actions.
- Add masking patterns for PII and financial data.
- Add responsive behavior for tablets and smaller screens.
- Add accessibility expectations: keyboard navigation, focus states, WCAG contrast, and form errors.

### Frontend

- Create new `vycepay-admin-portal`.
- Recommended stack: Next.js, TypeScript, Tailwind, shadcn/ui, TanStack Query, Zod, and React Hook Form.
- Do not store admin JWTs in `localStorage`.
- Prefer secure httpOnly cookies with server-side auth handling.
- Generate or maintain TypeScript API types from OpenAPI.
- Implement RBAC-aware navigation and route guards.

### Backend / API

- Create new `vycepay-admin-service`.
- Define `/api/admin/v1/**` OpenAPI contract before implementation.
- Add admin read APIs for dashboard, customers, KYC, wallets, transactions, callbacks, reports, audit, and health.
- Add controlled mutation APIs for customer status, wallet freeze, callback retry, roles, menus, and admin users.
- Add action-level authorization, not just menu visibility.
- Avoid exposing raw domain entities directly via REST; use DTOs and mappers.

### Data / Migrations

Need a new migration after existing files in `vycepay-database/src/main/resources/db/migration/`.

Proposed admin tables:

- `admin_user`
- `admin_role`
- `admin_permission`
- `admin_role_permission`
- `admin_menu`
- `admin_role_menu`
- `admin_session`
- `admin_password_reset_token`
- `admin_audit_log`
- Optional: `admin_approval_request` for maker-checker flows

Also add indexes for admin query patterns on existing tables, especially date, status, and search fields.

### Security / Compliance

Required for production fintech use:

- MFA for all admin accounts.
- Short-lived access sessions and refresh rotation.
- Account lockout after failed login attempts.
- Rate limiting on login, reset password, exports, and sensitive mutations.
- Separate admin JWT/session secret from customer JWT.
- PII masking by default.
- Explicit permission checks on every backend endpoint.
- Audit all admin actions and exports.
- Secure cookies: `HttpOnly`, `Secure`, `SameSite=Strict`.
- No raw secrets or sensitive data in logs.
- Restrict callback payload and KYC document access.

### Audit / Observability

- Keep admin audit separate from customer `activity_log`.
- Every mutation must log actor, target, action, reason, IP, user agent, correlation ID, before/after summary, timestamp, and result.
- Add structured logs and metrics for admin API usage.
- Add alerting for failed logins, permission denials, bulk exports, callback retry spikes, and suspicious admin activity.

### Testing / QA

Required test coverage:

- Migration tests.
- Admin auth tests.
- RBAC matrix tests.
- API integration tests.
- Service/repository tests for aggregate queries.
- Export masking tests.
- Audit log tests for every mutation.
- Frontend component/page tests.
- E2E tests for login, customer search, transaction detail, wallet freeze, callback retry, and role management.
- Accessibility checks.

### DevOps / Release

- Update `Dockerfile.services` for `vycepay-admin-service`.
- Add a frontend Dockerfile for `vycepay-admin-portal`.
- Update `docker-compose.yml` and `docker-compose.vycepay.yml`.
- Add CI pipeline for backend tests, frontend lint/typecheck/test/build, image build, and migration validation.
- Add production environment variables:
  - `ADMIN_JWT_SECRET`
  - `ADMIN_CORS_ORIGINS`
  - `ADMIN_SESSION_TTL`
  - `ADMIN_REFRESH_TTL`
  - `ADMIN_PORTAL_URL`
  - SMTP/email provider config
  - MFA provider config if externalized

## Recommended Target Architecture

### Backend

Create a new Spring Boot module:

- `vycepay-admin-service`
- Port: `8090`
- API prefix: `/api/admin/v1`
- Owns admin users, roles, permissions, sessions, and admin audit logs.
- Reads operational data from the shared MySQL database using read models/projections.
- Performs controlled writes only through explicit admin services with audit logging.

Recommended layers:

- `api/v1`: controllers and DTOs
- `application`: services/facades
- `domain`: admin domain models and policy objects
- `infrastructure`: repositories, integrations, health clients

### Frontend

Create a new app:

- `vycepay-admin-portal`
- Next.js + TypeScript
- Tailwind/shadcn using tokens extracted from `admin-portal-design/index.html`
- Server-aware auth using secure cookies
- Typed API client from OpenAPI
- Reusable components:
  - Data table
  - Status badge
  - KPI card
  - Chart wrappers
  - Confirm dialog
  - Date range picker
  - JSON viewer with redaction
  - Permission guard

### Data

Use shared MySQL, but isolate admin-owned tables with the `admin_*` prefix.

Operational tables remain owned by existing services:

- `customer`: auth/customer identity
- `kyc_verification`: KYC service
- `wallet`: wallet service
- `transaction`: transaction service
- `choice_bank_callback`: callback service
- `activity_log`: customer activity service

The admin service should not become a dumping ground for business logic. It should expose backoffice views and controlled admin actions.

## Implementation Roadmap

### Phase 0: Decisions and Contract

- Finalize roles and permissions.
- Finalize sensitive data masking rules.
- Finalize maker-checker requirements.
- Write OpenAPI contract for `/api/admin/v1/**`.
- Define audit event schema.

### Phase 1: Database and Admin Backend Foundation

- Add admin migration in `vycepay-database/src/main/resources/db/migration/`.
- Create `vycepay-admin-service`.
- Add module to root `pom.xml`.
- Configure datasource, Flyway compatibility, security, validation, and OpenAPI.
- Implement admin auth foundation, password hashing, sessions, and audit logging.

### Phase 2: Core Read APIs

- Dashboard summary and charts.
- Customer list/detail/activity.
- KYC list/detail.
- Wallet list/detail.
- Transaction list/detail/failed.
- Callback list/detail.
- Reports.
- System health.

### Phase 3: Controlled Mutation APIs

- Customer suspend/reactivate/deactivate.
- Wallet freeze/unfreeze/query balance.
- Callback retry.
- Admin menu/role/user management.
- Audit every action with reason capture.

### Phase 4: Frontend Foundation

- Bootstrap `vycepay-admin-portal`.
- Implement design tokens from `admin-portal-design/index.html`.
- Implement auth pages.
- Implement portal shell: sidebar, header, breadcrumb, profile/logout.
- Implement shared components and API client.

### Phase 5: Operational Screens

- Dashboard.
- Customers.
- KYC.
- Wallets.
- Transactions.
- Failed transactions.
- Callbacks.
- Audit log.

### Phase 6: Reports and Admin Management

- Transaction volume report.
- KYC funnel report.
- Customer growth report.
- System health.
- Menu management.
- Role management.
- Admin user management.

### Phase 7: Hardening and Release

- RBAC matrix tests.
- E2E tests.
- Security headers.
- Rate limiting.
- MFA.
- Export controls.
- Docker and deployment updates.
- Runbook and rollback plan.

## Production Readiness Checklist

- [ ] OpenAPI contract approved.
- [ ] Action-level RBAC implemented.
- [ ] MFA enabled for admin login.
- [ ] Admin sessions are short-lived and revocable.
- [ ] Secure httpOnly cookies used.
- [ ] Login rate limiting and lockout implemented.
- [ ] All admin mutations require reason capture.
- [ ] All admin mutations write to `admin_audit_log`.
- [ ] CSV exports are permissioned, audited, limited, and masked.
- [ ] PII masking applied consistently.
- [ ] Raw callback payload access restricted.
- [ ] KYC document access restricted and audited.
- [ ] System health endpoint restricted to authorized admins.
- [ ] Database indexes added for admin queries.
- [ ] Backend integration tests added.
- [ ] Frontend tests and accessibility checks added.
- [ ] Docker/compose files updated.
- [ ] CI/CD pipeline added.
- [ ] Production secrets managed outside code.
- [ ] Runbook and incident response notes documented.

## Required Decisions

1. Which admin roles are required for launch?
2. What exact permissions should each role have?
3. Should admin auth be local login, SSO, or both?
4. Is MFA mandatory from day one?
5. Should risky actions require maker-checker approval?
6. Can admins edit customer PII, or only view/mask it?
7. Who can view ID numbers, KYC documents, and raw callback payloads?
8. What fields must be masked in UI and CSV exports?
9. What are export size limits and retention requirements?
10. Should admin service read the shared DB directly or call service APIs for certain operations?
11. What production domain will host the admin portal?
12. What deployment target is expected: Docker Compose, VM, Kubernetes, or cloud platform?

## Risks

- Over-permissive admin access could expose PII and financial data.
- Menu-only RBAC can allow unauthorized backend actions if API permissions are not enforced.
- Raw callback payloads may contain sensitive data and should not be broadly visible.
- Direct DB reads can bypass service ownership boundaries if not carefully controlled.
- Callback retry can duplicate processing unless it is idempotent and audited.
- CSV exports can become a major data leakage vector.
- Lack of MFA and session revocation is high risk for backoffice access.
- Missing indexes can make dashboards/reports slow on production data volumes.

## Immediate Next Steps

1. Approve the target architecture: separate `vycepay-admin-service` and `vycepay-admin-portal`.
2. Finalize roles, permissions, masking rules, and maker-checker policy.
3. Produce the OpenAPI contract for `/api/admin/v1/**`.
4. Design the admin database migration with permissions, sessions, reset tokens, and audit tables.
5. Implement backend foundation first: auth, RBAC, audit, and read-only operational APIs.
6. Convert the static design from `admin-portal-design/index.html` into production Next.js components.
7. Add tests, security hardening, Docker/CI updates, and deployment runbooks before production release.
