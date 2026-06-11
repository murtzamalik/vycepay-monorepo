# VycePay Admin Portal — Complete Technical Plan

## Table of Contents
1. [DB Coverage Analysis](#1-db-coverage-analysis)
2. [V4 Migration — Admin Tables](#2-v4-migration)
3. [vycepay-admin-service (Backend)](#3-vycepay-admin-service)
4. [vycepay-admin-portal (Frontend)](#4-vycepay-admin-portal)
5. [Security Design](#5-security-design)
6. [Implementation Phases](#6-implementation-phases)
7. [Environment Variables](#7-environment-variables)
8. [Service Health URLs](#8-service-health-urls)

---

## 1. DB Coverage Analysis

### 1.1 Existing Tables vs Screens

| Screen | ID | Primary Tables | Coverage |
|--------|----|----------------|----------|
| Overview Dashboard | D1 | `customer`, `wallet`, `transaction`, `kyc_verification`, `choice_bank_callback` | ✅ |
| Customer List | C1 | `customer`, `wallet`, `kyc_verification` (LEFT JOIN) | ✅ |
| Customer Detail | C2 | `customer`, `kyc_verification`, `wallet`, `transaction`, `activity_log` | ✅ |
| Customer Edit/Actions | C3 | `customer` (UPDATE status) | ✅ |
| KYC List | K1 | `kyc_verification` JOIN `customer` | ✅ |
| KYC Detail | K2 | `kyc_verification` JOIN `customer` | ✅ |
| Wallet List | W1 | `wallet` JOIN `customer` | ✅ |
| Wallet Detail | W2 | `wallet` JOIN `customer`, `transaction` | ✅ |
| All Transactions | T1 | `transaction` JOIN `customer`, `wallet` | ✅ |
| Transaction Detail | T2 | `transaction` (incl. `error_code`, `error_msg`) | ✅ |
| Failed Transactions | T3 | `transaction` WHERE status='FAILED' | ✅ |
| Callback Log | CB1 | `choice_bank_callback` | ✅ |
| Callback Detail | CB2 | `choice_bank_callback` | ✅ |
| Volume Report | R1 | `transaction` (aggregate by date/type) | ✅ |
| KYC Funnel Report | R2 | `kyc_verification` (aggregate by status) | ✅ |
| Customer Growth Report | R3 | `customer` (aggregate by created_at) | ✅ |
| Audit Log | AL1 | `activity_log` (cross-customer, no X-Customer-Id filter) | ✅ |
| System Health | SH1 | Spring Boot Actuator HTTP calls — no DB | ✅ |
| Menu List / Create / Edit | M1, M2 | `admin_menu` | ✅ (V4) |
| Role List / Create / Edit | RL1, RL2 | `admin_role`, `admin_role_menu`, `admin_role_permission` | ✅ (V4) |
| Admin User List / Create / Edit | U1, U2 | `admin_user`, `admin_user_role` | ✅ (V4) |
| Login | A1 | `admin_user`, `admin_session` | ✅ (V4) |
| Forgot Password | A2 | `admin_user`, `admin_password_reset_token` | ✅ (V4) |

### 1.2 Code Gaps in Existing Services

| Gap | File | Resolution in Admin Service |
|-----|------|-----------------------------|
| `error_code` / `error_msg` absent from entity | `vycepay-transaction-service/.../Transaction.java` | Admin service defines its own `AdminTransaction` read model with all SQL columns |
| `ActivityController` filters by `X-Customer-Id` only | `vycepay-activity-service/.../ActivityController.java` | Admin service queries `activity_log` directly via JPA without that filter |

---

## 2. V4 Migration

**File:** `vycepay-database/src/main/resources/db/migration/V4__admin_tables.sql`

### 2.1 New Tables (9 total)

| Table | Purpose |
|-------|---------|
| `admin_user` | Portal operator identity; includes lockout + MFA columns |
| `admin_menu` | All navigable portal routes |
| `admin_permission` | Atomic action codes e.g. `customer:suspend`, `wallet:freeze` |
| `admin_role` | Named permission groups |
| `admin_role_menu` | Which menus a role can see (sidebar visibility) |
| `admin_role_permission` | Which action codes a role holds (API-level enforcement) |
| `admin_user_role` | User → role assignments |
| `admin_session` | Revocable sessions; JWT `jti` stored here |
| `admin_password_reset_token` | One-time forgot-password tokens (hashed) |
| `admin_audit_log` | Immutable record of every admin mutation |

### 2.2 Added Indexes on Existing Tables

| Table | New Index | Purpose |
|-------|-----------|---------|
| `customer` | `idx_admin_status_created (status, created_at)` | Customer list with status filter + date range |
| `transaction` | `idx_admin_status_type_created (status, type, created_at)` | Transaction list/export queries |
| `transaction` | `idx_admin_error_code (error_code)` | Failed transactions grouped by error |
| `choice_bank_callback` | `idx_admin_processed_created (processed, created_at)` | Dashboard unprocessed alert |
| `choice_bank_callback` | `idx_admin_type_processed (notification_type, processed)` | Callback list filter |
| `kyc_verification` | `idx_admin_status_updated (status, updated_at)` | KYC list filter |
| `activity_log` | `idx_admin_action_created (action, created_at)` | Cross-customer audit log |

### 2.3 Seed Data

The migration seeds:
- **16 menus** covering all portal routes
- **17 permission codes** across all domains
- **4 roles:** `SUPER_ADMIN`, `OPERATIONS`, `FINANCE`, `SUPPORT`
- **Role–menu assignments** per the access matrix below
- **Role–permission assignments** per the access matrix below

### 2.4 Role Access Matrix

| Permission | SUPER_ADMIN | OPERATIONS | FINANCE | SUPPORT |
|------------|:-----------:|:----------:|:-------:|:-------:|
| `customer:view` | ✅ | ✅ | ✅ | ✅ |
| `customer:view_pii` | ✅ | ✅ | ❌ | ❌ |
| `customer:suspend` | ✅ | ✅ | ❌ | ❌ |
| `wallet:view` | ✅ | ✅ | ✅ | ❌ |
| `wallet:freeze` | ✅ | ✅ | ❌ | ❌ |
| `transaction:view` | ✅ | ✅ | ✅ | ✅ |
| `transaction:export` | ✅ | ✅ | ✅ | ❌ |
| `callback:view` | ✅ | ✅ | ❌ | ❌ |
| `callback:retry` | ✅ | ✅ | ❌ | ❌ |
| `kyc:view` | ✅ | ✅ | ✅ | ✅ |
| `kyc:view_documents` | ✅ | ✅ | ❌ | ❌ |
| `report:view` | ✅ | ✅ | ✅ | ❌ |
| `audit_log:view` | ✅ | ✅ | ✅ | ❌ |
| `system:health` | ✅ | ✅ | ❌ | ❌ |
| `admin:manage_menus` | ✅ | ❌ | ❌ | ❌ |
| `admin:manage_roles` | ✅ | ❌ | ❌ | ❌ |
| `admin:manage_users` | ✅ | ❌ | ❌ | ❌ |

---

## 3. vycepay-admin-service

### 3.1 Bootstrap

| Item | Value |
|------|-------|
| Module path | `vycepay-admin-service/` |
| Port | `8090` |
| API prefix | `/api/admin/v1` |
| Add to root `pom.xml` | `<module>vycepay-admin-service</module>` |
| DB | Same MySQL instance; reads operational tables, writes only `admin_*` tables |

**Key dependencies:**
```xml
<dependencies>
  <dependency><groupId>com.vycepay</groupId><artifactId>vycepay-common</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId></dependency>
  <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId></dependency>
  <dependency><groupId>dev.samstevens.totp</groupId><artifactId>totp-spring-boot-starter</artifactId></dependency>
</dependencies>
```

### 3.2 Layer Structure

```
vycepay-admin-service/src/main/java/com/vycepay/admin/
├── AdminServiceApplication.java
│
├── config/
│   ├── AdminSecurityConfig.java        # JWT filter chain, CORS, @EnableMethodSecurity
│   ├── AdminJwtProperties.java         # admin JWT secret + expiry from env
│   ├── RateLimitConfig.java            # Bucket4j rate limiting (login, exports)
│   └── WebClientConfig.java            # RestTemplate for Actuator health checks
│
├── api/v1/
│   ├── AuthController.java             # /auth/**
│   ├── DashboardController.java        # /dashboard/**
│   ├── CustomerController.java         # /customers/**
│   ├── KycController.java              # /kyc/**
│   ├── WalletController.java           # /wallets/**
│   ├── TransactionController.java      # /transactions/**
│   ├── CallbackController.java         # /callbacks/**
│   ├── ReportController.java           # /reports/**
│   ├── AuditLogController.java         # /audit-log
│   ├── SystemHealthController.java     # /system-health
│   ├── MenuController.java             # /menus/**
│   ├── RoleController.java             # /roles/**
│   └── AdminUserController.java        # /users/**
│   └── dto/                            # Request/Response DTOs (never expose entities)
│
├── application/service/
│   ├── AdminAuthService.java           # login, MFA verify, logout, forgot/reset password
│   ├── AdminSessionService.java        # create/validate/revoke sessions via admin_session
│   ├── AdminPermissionEvaluator.java   # Spring PermissionEvaluator for @PreAuthorize
│   ├── AdminAuditService.java          # write to admin_audit_log after every mutation
│   ├── CustomerAdminService.java
│   ├── KycAdminService.java
│   ├── WalletAdminService.java
│   ├── TransactionAdminService.java
│   ├── CallbackAdminService.java
│   ├── ReportService.java
│   ├── AuditLogService.java
│   ├── SystemHealthService.java        # parallel Actuator calls
│   ├── MenuService.java
│   ├── RoleService.java
│   └── AdminUserService.java
│
├── domain/model/
│   ├── AdminUser.java
│   ├── AdminRole.java
│   ├── AdminMenu.java
│   ├── AdminPermission.java
│   ├── AdminRoleMenu.java
│   ├── AdminRolePermission.java
│   ├── AdminUserRole.java
│   ├── AdminSession.java
│   ├── AdminPasswordResetToken.java
│   ├── AdminAuditLog.java
│   ├── AdminCustomer.java              # Read-only mirror of customer table
│   ├── AdminKycVerification.java       # Read-only mirror
│   ├── AdminWallet.java                # Read-only mirror
│   ├── AdminTransaction.java           # Read-only mirror — includes error_code, error_msg
│   ├── AdminChoiceBankCallback.java    # Read-only mirror
│   └── AdminActivityLog.java           # Read-only mirror
│
└── infrastructure/persistence/
    ├── AdminUserRepository.java
    ├── AdminRoleRepository.java
    ├── AdminMenuRepository.java
    ├── AdminPermissionRepository.java
    ├── AdminSessionRepository.java
    ├── AdminPasswordResetTokenRepository.java
    ├── AdminAuditLogRepository.java
    ├── CustomerAdminRepository.java
    ├── KycAdminRepository.java
    ├── WalletAdminRepository.java
    ├── TransactionAdminRepository.java
    ├── CallbackAdminRepository.java
    └── ActivityLogAdminRepository.java
```

### 3.3 Full REST API Contract

#### Auth — `/api/admin/v1/auth`

| Method | Path | Request | Response | Notes |
|--------|------|---------|----------|-------|
| POST | `/login` | `{username, password}` | `{token, jti, adminUser, menus[]}` | BCrypt verify → issue JWT → create `admin_session` row |
| POST | `/login/mfa` | `{jti, totpCode}` | `{token, adminUser, menus[]}` | Verify TOTP; upgrade session to fully authenticated |
| POST | `/logout` | — | 200 | Revoke `admin_session` row by jti from JWT |
| POST | `/forgot-password` | `{email}` | 200 | Hash token → save to `admin_password_reset_token` → email link |
| POST | `/reset-password` | `{token, newPassword}` | 200 | SHA-256(token) lookup → validate expiry → update hash → mark used |
| GET | `/me` | — | `{adminUser, menus[], permissions[]}` | Resolved from JWT; used to refresh sidebar |

#### Dashboard — `/api/admin/v1/dashboard`

| Method | Path | Query Params | Response |
|--------|------|-------------|----------|
| GET | `/summary` | — | `{totalCustomers, activeWallets, todayTxVolume(KES), todayTxCount, kycApprovalRate, pendingCallbacks, stuckTxOver1h}` |
| GET | `/tx-volume-chart` | `days=30` | `[{date, transferAmount, depositAmount}]` |
| GET | `/tx-type-donut` | `days=30` | `{transferCount, depositCount, transferVolume, depositVolume}` |
| GET | `/kyc-status-chart` | — | `[{status, count}]` |
| GET | `/alerts` | — | `{unprocessedCallbacks, pendingTxOver1h, pendingTxOver24h}` |
| GET | `/recent-transactions` | `limit=10` | `[TransactionSummary]` |

#### Customers — `/api/admin/v1/customers`

| Method | Path | Params / Body | Notes |
|--------|------|--------------|-------|
| GET | `/` | `page, size, search, status, startDate, endDate` | PII masked unless `customer:view_pii` permission |
| GET | `/{customerId}` | — | PII masking applied per caller's permissions |
| PATCH | `/{customerId}/status` | `{status, reason}` | Requires `customer:suspend`; logs to `admin_audit_log` |
| GET | `/{customerId}/transactions` | `page, size, type, status` | |
| GET | `/{customerId}/kyc` | — | Documents only visible with `kyc:view_documents` |
| GET | `/{customerId}/activity` | `page, size` | |
| GET | `/export` | same as list | Requires `transaction:export`; max 10,000 rows; audited |

#### KYC — `/api/admin/v1/kyc`

| Method | Path | Params | Notes |
|--------|------|--------|-------|
| GET | `/` | `page, size, status, search, startDate, endDate` | |
| GET | `/{kycId}` | — | Document URLs only in response if caller has `kyc:view_documents` |

#### Wallets — `/api/admin/v1/wallets`

| Method | Path | Params / Body | Notes |
|--------|------|--------------|-------|
| GET | `/` | `page, size, search, status` | |
| GET | `/{walletId}` | — | |
| PATCH | `/{walletId}/status` | `{status, reason}` | Requires `wallet:freeze`; logs to `admin_audit_log` |

#### Transactions — `/api/admin/v1/transactions`

| Method | Path | Params | Notes |
|--------|------|--------|-------|
| GET | `/` | `page, size, type, status, customerId, startDate, endDate, search` | |
| GET | `/{txId}` | — | |
| GET | `/failed` | `page, size, errorCode, startDate, endDate` | |
| GET | `/export` | same as list | Requires `transaction:export`; max 10,000 rows; audited |

#### Callbacks — `/api/admin/v1/callbacks`

| Method | Path | Params / Body | Notes |
|--------|------|--------------|-------|
| GET | `/` | `page, size, notificationType, processed, startDate, endDate` | Raw payload only with `callback:view` |
| GET | `/{cbId}` | — | |
| POST | `/{cbId}/retry` | — | Requires `callback:retry`; audited; idempotent |

#### Reports — `/api/admin/v1/reports`

| Method | Path | Params | Response |
|--------|------|--------|----------|
| GET | `/volume` | `startDate, endDate, groupBy=day\|week\|month` | `[{period, transferVol, depositVol, txCount}]` |
| GET | `/kyc-funnel` | `startDate, endDate` | `{submitted, otpSent, otpConfirmed, pending, approved, rejected}` |
| GET | `/customer-growth` | `startDate, endDate, groupBy=day\|week\|month` | `[{period, newCustomers, cumulativeTotal, activeWallets}]` |

#### Audit Log — `/api/admin/v1/audit-log`

| Method | Path | Params |
|--------|------|--------|
| GET | `/` | `page, size, customerId, action, startDate, endDate` |
| GET | `/export` | same as list; audited |

#### System Health — `/api/admin/v1/system-health`

| Method | Path | Response |
|--------|------|----------|
| GET | `/` | `{services:[{name, port, status, responseTimeMs, circuitBreakerState, lastChecked}], choiceBank:{reachable, latencyMs}}` |

_`SystemHealthService` makes parallel HTTP GET calls to `http://localhost:{port}/actuator/health`. Returns `DOWN` on timeout without throwing. Circuit breaker state read from Actuator `/actuator/circuitbreakers`._

#### Menus — `/api/admin/v1/menus`

| Method | Path | Body | Notes |
|--------|------|------|-------|
| GET | `/` | — | Returns nested tree (parent items with children array) |
| POST | `/` | `{name, route, icon, parentId, sortOrder}` | Requires `admin:manage_menus` |
| PUT | `/{menuId}` | same fields | Requires `admin:manage_menus` |
| DELETE | `/{menuId}` | — | Requires `admin:manage_menus`; blocked if menu has active role assignments |

#### Roles — `/api/admin/v1/roles`

| Method | Path | Body | Notes |
|--------|------|------|-------|
| GET | `/` | — | List with menuCount and permissionCount |
| GET | `/{roleId}` | — | Role + assigned menuIds + assigned permissionCodes |
| POST | `/` | `{name, description, menuIds[], permissionCodes[]}` | Requires `admin:manage_roles` |
| PUT | `/{roleId}` | same | Requires `admin:manage_roles` |
| DELETE | `/{roleId}` | — | Requires `admin:manage_roles`; blocked if role has active user assignments |

#### Admin Users — `/api/admin/v1/users`

| Method | Path | Body | Notes |
|--------|------|------|-------|
| GET | `/` | — | `Page<AdminUserSummary>` |
| GET | `/{userId}` | — | `AdminUserDetail` (roles included) |
| POST | `/` | `{username, email, fullName, password, roleIds[]}` | Requires `admin:manage_users`; BCrypt password |
| PUT | `/{userId}` | `{fullName, email, roleIds[], status}` | Requires `admin:manage_users` |
| POST | `/{userId}/reset-password` | `{newPassword}` | Requires `admin:manage_users`; audited |

---

## 4. vycepay-admin-portal

### 4.1 Project Bootstrap

```
vycepay-admin-portal/
├── package.json          # next@15, react@19, typescript, tailwindcss, @shadcn/ui,
│                         # recharts, axios, @tanstack/react-query,
│                         # react-hook-form, zod, date-fns, lucide-react,
│                         # zustand, js-cookie, @types/js-cookie
├── next.config.ts
├── tailwind.config.ts    # custom tokens (see §4.4)
└── src/
    ├── app/
    │   ├── layout.tsx                         # root: font + ReactQueryProvider
    │   ├── (auth)/
    │   │   ├── login/page.tsx                 # A1
    │   │   └── forgot-password/page.tsx       # A2
    │   └── (portal)/
    │       ├── layout.tsx                     # AppShell: Sidebar + TopHeader
    │       ├── dashboard/page.tsx             # D1
    │       ├── customers/
    │       │   ├── page.tsx                   # C1
    │       │   └── [id]/
    │       │       ├── page.tsx               # C2
    │       │       └── edit/page.tsx          # C3
    │       ├── kyc/
    │       │   ├── page.tsx                   # K1
    │       │   └── [id]/page.tsx              # K2
    │       ├── wallets/
    │       │   ├── page.tsx                   # W1
    │       │   └── [id]/page.tsx              # W2
    │       ├── transactions/
    │       │   ├── page.tsx                   # T1
    │       │   ├── failed/page.tsx            # T3
    │       │   └── [id]/page.tsx              # T2
    │       ├── callbacks/
    │       │   ├── page.tsx                   # CB1
    │       │   └── [id]/page.tsx              # CB2
    │       ├── reports/
    │       │   ├── volume/page.tsx            # R1
    │       │   ├── kyc-funnel/page.tsx        # R2
    │       │   └── growth/page.tsx            # R3
    │       ├── audit-log/page.tsx             # AL1
    │       ├── system-health/page.tsx         # SH1
    │       └── admin/
    │           ├── menus/
    │           │   ├── page.tsx               # M1
    │           │   └── [id]/page.tsx          # M2
    │           ├── roles/
    │           │   ├── page.tsx               # RL1
    │           │   └── [id]/page.tsx          # RL2
    │           └── users/
    │               ├── page.tsx               # U1
    │               └── [id]/page.tsx          # U2
    ├── components/
    │   ├── layout/
    │   │   ├── AppShell.tsx
    │   │   ├── Sidebar.tsx               # Dynamic from useAuthStore menus
    │   │   ├── SidebarItem.tsx           # With optional collapsible children
    │   │   ├── TopHeader.tsx             # Breadcrumb + admin name + logout
    │   │   └── Breadcrumb.tsx
    │   ├── shared/
    │   │   ├── DataTable.tsx             # Columns, data, pagination, search, filters slot, export button
    │   │   ├── StatusBadge.tsx           # SUCCESS/FAILED/PENDING/PROCESSING semantic colors
    │   │   ├── KpiCard.tsx               # Value, label, trend, sparkline
    │   │   ├── SkeletonTable.tsx         # Loading state for data tables
    │   │   ├── EmptyState.tsx            # Empty list with icon + message
    │   │   ├── ConfirmDialog.tsx         # Destructive action modal with reason input
    │   │   ├── DateRangePicker.tsx       # Popover + Calendar, global range
    │   │   ├── PageHeader.tsx            # Title + description + right action slot
    │   │   ├── ErrorBanner.tsx           # API error display
    │   │   ├── JsonViewer.tsx            # Formatted + syntax-highlighted JSON
    │   │   └── PermissionGuard.tsx       # Renders children only if user has permission code
    │   └── charts/
    │       ├── AreaChart.tsx             # Recharts ResponsiveContainer + AreaChart
    │       ├── StackedBarChart.tsx       # Recharts BarChart stacked
    │       ├── DonutChart.tsx            # Recharts PieChart
    │       └── MultiLineChart.tsx        # Recharts multi-series LineChart
    ├── lib/
    │   ├── api/
    │   │   ├── client.ts                 # Axios: baseURL from env, Bearer token, 401 redirect
    │   │   ├── auth.api.ts
    │   │   ├── dashboard.api.ts
    │   │   ├── customers.api.ts
    │   │   ├── kyc.api.ts
    │   │   ├── wallets.api.ts
    │   │   ├── transactions.api.ts
    │   │   ├── callbacks.api.ts
    │   │   ├── reports.api.ts
    │   │   ├── audit-log.api.ts
    │   │   ├── system-health.api.ts
    │   │   ├── menus.api.ts
    │   │   ├── roles.api.ts
    │   │   └── admin-users.api.ts
    │   ├── hooks/                        # TanStack Query hooks per domain
    │   │   ├── useCustomers.ts
    │   │   ├── useTransactions.ts
    │   │   └── ...
    │   ├── store/
    │   │   └── useAuthStore.ts           # Zustand: token, adminUser, menus[], permissions[], logout()
    │   ├── utils/
    │   │   ├── formatCurrency.ts         # Intl.NumberFormat('en-KE', {currency:'KES'})
    │   │   ├── formatDate.ts             # date-fns helpers
    │   │   ├── maskPii.ts                # Mobile last-4, ID number masking
    │   │   └── csvExport.ts              # Blob → download trigger
    │   └── types/
    │       ├── api.types.ts              # All request/response types matching backend DTOs
    │       └── auth.types.ts
    └── middleware.ts                     # Redirect to /login if no token cookie
```

### 4.2 Auth Middleware

```typescript
// src/middleware.ts
import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

const PUBLIC_PATHS = ['/login', '/forgot-password', '/reset-password']

export function middleware(request: NextRequest) {
  const token = request.cookies.get('admin_token')?.value
  const isPublic = PUBLIC_PATHS.some(p => request.nextUrl.pathname.startsWith(p))
  if (!token && !isPublic) {
    return NextResponse.redirect(new URL('/login', request.url))
  }
  if (token && request.nextUrl.pathname === '/login') {
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }
  return NextResponse.next()
}

export const config = { matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'] }
```

### 4.3 API Client

```typescript
// src/lib/api/client.ts
import axios from 'axios'
import Cookies from 'js-cookie'

const client = axios.create({
  baseURL: process.env.NEXT_PUBLIC_ADMIN_API_URL ?? 'http://localhost:8090',
})

client.interceptors.request.use(config => {
  const token = Cookies.get('admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  r => r,
  error => {
    if (error.response?.status === 401) {
      Cookies.remove('admin_token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default client
```

### 4.4 Design Tokens (Tailwind)

```typescript
// tailwind.config.ts — extend.colors
colors: {
  bg: {
    primary:  '#070912',
    surface:  '#0F1221',
    elevated: '#1B2040',
    sidebar:  '#0B0D18',
  },
  brand:   { DEFAULT: '#4F79FF', hover: '#3D63E6' },
  success: '#0FD67C',
  warning: '#F5A623',
  danger:  '#FF3D57',
  purple:  '#8B5CF6',
  border:  { DEFAULT: '#1B2040', subtle: '#262D4A' },
  text: {
    primary:   '#E8EAF0',
    secondary: '#8B91A8',
    muted:     '#545B73',
  },
}
// fontFamily: { sans: ['Inter'], mono: ['JetBrains Mono'] }
```

### 4.5 PermissionGuard Component

```typescript
// components/shared/PermissionGuard.tsx
// Hides children if the current admin lacks the required permission code.
// Use this to conditionally render action buttons (Suspend, Freeze, Retry, Export).

interface PermissionGuardProps {
  permission: string          // e.g. "customer:suspend"
  children: React.ReactNode
  fallback?: React.ReactNode  // optional replacement (e.g. disabled button)
}
```

### 4.6 DataTable Component Contract

```typescript
interface DataTableProps<T> {
  columns: ColumnDef<T>[]
  data: T[]
  loading: boolean
  pagination: { page: number; size: number; total: number }
  onPageChange: (page: number) => void
  onSearch?: (query: string) => void
  filters?: React.ReactNode       // slot for custom filter controls
  onExport?: () => void           // only rendered if caller passes this prop
  emptyMessage?: string
}
```

---

## 5. Security Design

### 5.1 Two Separate JWT Secrets

| JWT | Env Var | Issuer | Audience |
|-----|---------|--------|----------|
| Mobile JWT | `JWT_SECRET` | `vycepay-auth` | Mobile BFF and all customer services |
| Admin JWT | `ADMIN_JWT_SECRET` | `vycepay-admin` | Admin service only |

Admin JWT payload: `{ sub: externalId, username, jti: uuid, iat, exp }`  
Expiry: **15 minutes by default** (`ADMIN_JWT_EXPIRATION_MS=900000`)  
The JWT carries only identity. Permissions are resolved server-side on each request from `admin_role_permission` via the `jti` → `admin_session` lookup.

### 5.2 Authentication Flow

```
POST /auth/login
  → verify password (BCrypt)
  → if MFA enabled: create inactive revocable session, return {jti}
  → client POSTs /auth/login/mfa with {jti, totpCode}
  → verify standards-compatible TOTP → activate admin_session row → sign JWT (contains jti)
  → return JWT in response body; frontend stores in httpOnly cookie

Every authenticated request:
  → AdminJwtFilter extracts jti from JWT
  → AdminSessionService: SELECT from admin_session WHERE jti=? AND revoked=FALSE AND expires_at > NOW()
  → if not found or revoked: 401
  → load admin_user + roles + permissions into SecurityContext
  → proceed to controller

POST /auth/logout
  → UPDATE admin_session SET revoked=TRUE WHERE jti=?
```

Password reset tokens are generated as high-entropy random values, stored only as SHA-256 hashes, and never returned from the API. A production deployment must configure a real delivery adapter; the development fallback logs only non-sensitive metadata.

### 5.3 Action-Level Authorization

Every mutating endpoint is guarded by `@PreAuthorize` using a custom `AdminPermissionEvaluator`:

```java
// Examples
@PreAuthorize("hasAdminPermission('customer:suspend')")
public ResponseEntity<?> updateCustomerStatus(...) { ... }

@PreAuthorize("hasAdminPermission('wallet:freeze')")
public ResponseEntity<?> updateWalletStatus(...) { ... }

@PreAuthorize("hasAdminPermission('callback:retry')")
public ResponseEntity<?> retryCallback(...) { ... }

@PreAuthorize("hasAdminPermission('admin:manage_users')")
public ResponseEntity<?> createAdminUser(...) { ... }
```

`AdminPermissionEvaluator` resolves the caller's permissions from the `SecurityContext` (loaded during JWT filter) and checks against the `admin_role_permission` table.

### 5.4 Login Security

- **Max attempts:** 5 failed logins → lock account for 15 minutes (`locked_until` column)
- **Reset on success:** clear `failed_login_attempts` and `locked_until`
- **Rate limiting (Bucket4j):** max 10 login attempts per minute per IP
- **Rate limiting on exports:** max 5 export requests per minute per admin user

### 5.5 PII Masking Rules

| Field | Default Display | With `customer:view_pii` |
|-------|----------------|--------------------------|
| Mobile number | `+254 *** *** 78` (last 2 digits) | Full number |
| ID number | `*******` | Full value |
| KYC document URLs | Not returned | Returned in response |
| Wallet balance | Visible to FINANCE + SUPER_ADMIN | N/A |

Masking is applied in the **service layer** before DTO mapping, not in the frontend.

### 5.6 Admin Audit Logging

Every mutation writes to `admin_audit_log` via `AdminAuditService.log(...)`. Required for:

| Action | reason required? |
|--------|:-:|
| `SUSPEND_CUSTOMER` | ✅ |
| `REACTIVATE_CUSTOMER` | ✅ |
| `FREEZE_WALLET` | ✅ |
| `UNFREEZE_WALLET` | ✅ |
| `RETRY_CALLBACK` | ❌ |
| `CREATE_ADMIN_USER` | ❌ |
| `RESET_ADMIN_PASSWORD` | ❌ |
| `UPDATE_ROLE` | ❌ |
| `EXPORT_TRANSACTIONS` | ❌ (auto-logged) |

### 5.7 CORS Configuration

```properties
vycepay.admin.cors.allowed-origins=http://localhost:3000,https://admin.vycepay.com
```

---

## 6. Implementation Phases

### Phase 1 — DB + Admin Service Foundation (Week 1–2)
**Goal:** Admin service boots, login returns token + menus + permissions

1. Add `V4__admin_tables.sql` to `vycepay-database`
2. Create `vycepay-admin-service` Maven module; add to root `pom.xml`
3. `application.yml` — port 8090, same DB, separate `ADMIN_JWT_SECRET`
4. JPA entities: all `admin_*` domain models
5. `AdminSecurityConfig` + `AdminJwtFilter` + `AdminPermissionEvaluator`
6. `AdminSessionService` — create/validate/revoke via `admin_session`
7. `AdminAuthService` + `AuthController` — login, MFA, logout, forgot/reset password
8. Read-only operational domain mirrors (Customer, Wallet, Transaction with error fields, KycVerification, ChoiceBankCallback, ActivityLog)

**Done when:** `POST /api/admin/v1/auth/login` returns `{token, adminUser, menus[], permissions[]}`

---

### Phase 2 — Core Read APIs (Week 3–4)
**Goal:** All read-heavy screens can be populated from real data

9. `DashboardController` + `DashboardService` — KPI summary, all chart data, alerts, recent transactions
10. `CustomerController` + `CustomerAdminService` — list (PII masked), detail, sub-resources
11. `KycController` + `KycAdminService` — list, detail (documents permission-gated)
12. `WalletController` + `WalletAdminService` — list, detail
13. `TransactionController` + `TransactionAdminService` — list, detail, failed list, CSV export
14. `CallbackController` + `CallbackAdminService` — list, detail (payload permission-gated)
15. `AuditLogController` — cross-customer `activity_log` query

**Done when:** All operational list and detail screens return real data in Postman

---

### Phase 3 — Reports, Health, Admin CRUD (Week 5)
**Goal:** Full backend coverage

16. `ReportController` + `ReportService` — volume, KYC funnel, growth (aggregate queries)
17. `SystemHealthController` + `SystemHealthService` — parallel Actuator calls
18. `MenuController` + `MenuService` — CRUD + tree builder
19. `RoleController` + `RoleService` — CRUD + menu + permission assignment
20. `AdminUserController` + `AdminUserService` — CRUD + role assignment + password reset
21. `AdminAuditService` wired into all mutating service methods

**Done when:** Full backend; every endpoint covered in Postman collection

---

### Phase 4 — Frontend Shell + Auth (Week 6)
**Goal:** Navigable portal with real login, sidebar from API

22. Bootstrap `vycepay-admin-portal` (Next.js 15 + Tailwind + shadcn/ui)
23. Design tokens in `tailwind.config.ts`
24. Auth pages: Login (A1), Forgot Password (A2)
25. `useAuthStore` (Zustand), `client.ts` (Axios), `middleware.ts`
26. `AppShell`, `Sidebar` (menus from auth store), `TopHeader`, `Breadcrumb`
27. Shared components: `DataTable`, `StatusBadge`, `KpiCard`, `SkeletonTable`, `EmptyState`, `ConfirmDialog`, `PageHeader`, `PermissionGuard`, chart wrappers

**Done when:** Login → dashboard shell renders, sidebar fully navigable, `PermissionGuard` hides/shows elements

---

### Phase 5 — Operational Screens (Week 7–8)
**Goal:** Ops team can use the portal end-to-end

28. Dashboard (D1) — KPI cards, 3 charts, alerts panel, recent transactions
29. Customer List (C1) + Detail (C2) + Edit/Actions (C3)
30. KYC List (K1) + Detail (K2)
31. Wallet List (W1) + Detail (W2)
32. All Transactions (T1) + Detail (T2) + Failed (T3)
33. Callback Log (CB1) + Detail (CB2)
34. Audit Log (AL1)

**Done when:** Core operations fully functional with real data, error + empty + loading states all working

---

### Phase 6 — Reports, Health, Admin Management (Week 9)
**Goal:** Portal feature-complete

35. Volume Report (R1), KYC Funnel (R2), Growth Report (R3)
36. System Health (SH1)
37. Menu Management (M1, M2)
38. Role Management (RL1, RL2) — hierarchical checklist + permission checkboxes + preview panel
39. Admin User Management (U1, U2)

**Done when:** All 26 screens live

---

### Phase 7 — Hardening (Week 10)
**Goal:** Production-ready

40. RBAC matrix tests (each role × each endpoint)
41. Login lockout + rate limiting tests
42. Audit log coverage tests (every mutation has a log entry)
43. PII masking tests (FINANCE role cannot see unmasked mobile/ID)
44. Export size limit enforcement
45. Security headers (`Strict-Transport-Security`, `X-Content-Type-Options`, `X-Frame-Options`)
46. Add `vycepay-admin-service` to `Dockerfile.services` and `docker-compose.vycepay.yml`
47. Add `vycepay-admin-portal` Dockerfile + nginx config
48. CI pipeline: backend tests → frontend lint/typecheck/test/build → image build → migration validation

---

## 7. Environment Variables

| Variable | Service | Description | Default |
|----------|---------|-------------|---------|
| `ADMIN_JWT_SECRET` | admin-service | Signing secret — must differ from `JWT_SECRET` | — (required) |
| `ADMIN_JWT_EXPIRATION_MS` | admin-service | Token lifetime | `28800000` (8h) |
| `ADMIN_CORS_ORIGINS` | admin-service | Comma-separated allowed origins | `http://localhost:3000` |
| `ADMIN_LOGIN_MAX_ATTEMPTS` | admin-service | Lockout threshold | `5` |
| `ADMIN_LOCKOUT_MINUTES` | admin-service | Lockout duration | `15` |
| `ADMIN_EXPORT_MAX_ROWS` | admin-service | Max rows per CSV export | `10000` |
| `NEXT_PUBLIC_ADMIN_API_URL` | admin-portal | Backend base URL | `http://localhost:8090` |
| `ADMIN_SMTP_HOST` | admin-service | SMTP for password reset emails | — |
| `ADMIN_SMTP_FROM` | admin-service | From address | `noreply@vycepay.com` |

All other DB/service config reuses existing variables (`DB_HOST`, `DB_USER`, `DB_PASS`, etc.)

---

## 8. Service Health URLs

| Service | Actuator Endpoint |
|---------|------------------|
| BFF | `http://localhost:8080/actuator/health` |
| Callback | `http://localhost:8081/actuator/health` |
| Auth | `http://localhost:8082/actuator/health` |
| KYC | `http://localhost:8083/actuator/health` |
| Wallet | `http://localhost:8084/actuator/health` |
| Transaction | `http://localhost:8085/actuator/health` |
| Activity | `http://localhost:8086/actuator/health` |
| Choice Bank | `https://baas-pilot.choicebankapi.com/` (HEAD request) |

Circuit breaker states: `http://localhost:{port}/actuator/circuitbreakers`
