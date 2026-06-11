-- V4: Admin Portal — RBAC, sessions, audit, and performance indexes
-- Tables owned exclusively by vycepay-admin-service (prefix: admin_*)

-- ─────────────────────────────────────────────────────────────────────────────
-- ADMIN IDENTITY
-- ─────────────────────────────────────────────────────────────────────────────

-- admin_user: Portal operators and administrators
CREATE TABLE admin_user (
  id                     BIGINT       PRIMARY KEY AUTO_INCREMENT,
  external_id            VARCHAR(64)  UNIQUE NOT NULL,
  username               VARCHAR(64)  UNIQUE NOT NULL,
  email                  VARCHAR(255) UNIQUE NOT NULL,
  password_hash          VARCHAR(256) NOT NULL,
  full_name              VARCHAR(128),
  status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',    -- ACTIVE | SUSPENDED
  failed_login_attempts  INT          NOT NULL DEFAULT 0,
  locked_until           TIMESTAMP    NULL,
  mfa_secret             VARCHAR(128) NULL,                         -- TOTP secret (encrypted at rest)
  mfa_enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
  last_login_at          TIMESTAMP    NULL,
  created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_email   (email),
  KEY idx_status  (status)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- MENU STRUCTURE
-- ─────────────────────────────────────────────────────────────────────────────

-- admin_menu: All navigable routes in the admin portal
CREATE TABLE admin_menu (
  id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL,          -- Display name, e.g. "Customers"
  route       VARCHAR(128) NOT NULL,          -- Frontend route, e.g. "/customers"
  icon        VARCHAR(64)  NULL,              -- Lucide icon name, e.g. "users"
  parent_id   BIGINT       NULL,              -- NULL = top-level section
  sort_order  INT          NOT NULL DEFAULT 0,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (parent_id) REFERENCES admin_menu(id),
  UNIQUE KEY uk_route (route)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- ACTION-LEVEL PERMISSIONS
-- ─────────────────────────────────────────────────────────────────────────────

-- admin_permission: Atomic action codes checked on every API call
-- Format: "domain:action", e.g. "customer:suspend", "wallet:freeze"
CREATE TABLE admin_permission (
  id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  code        VARCHAR(64)  UNIQUE NOT NULL,
  description VARCHAR(256) NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────────────────────
-- ROLES AND ASSIGNMENTS
-- ─────────────────────────────────────────────────────────────────────────────

-- admin_role: Named permission groups (e.g. SUPER_ADMIN, OPERATIONS, FINANCE, SUPPORT)
CREATE TABLE admin_role (
  id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64)  UNIQUE NOT NULL,
  description VARCHAR(256) NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- admin_role_menu: Which menu items a role can see (controls sidebar visibility)
CREATE TABLE admin_role_menu (
  role_id  BIGINT NOT NULL,
  menu_id  BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id),
  FOREIGN KEY (role_id) REFERENCES admin_role(id) ON DELETE CASCADE,
  FOREIGN KEY (menu_id) REFERENCES admin_menu(id) ON DELETE CASCADE
);

-- admin_role_permission: Which action-level permissions a role holds
-- Enforced on every backend endpoint via @PreAuthorize
CREATE TABLE admin_role_permission (
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  FOREIGN KEY (role_id)       REFERENCES admin_role(id)       ON DELETE CASCADE,
  FOREIGN KEY (permission_id) REFERENCES admin_permission(id) ON DELETE CASCADE
);

-- admin_user_role: Which roles an admin user holds
CREATE TABLE admin_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES admin_user(id) ON DELETE CASCADE,
  FOREIGN KEY (role_id) REFERENCES admin_role(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────────────────────
-- SESSION MANAGEMENT
-- ─────────────────────────────────────────────────────────────────────────────

-- admin_session: Revocable sessions — JWT jti stored here
-- On each authenticated request: verify jti exists and revoked=FALSE
CREATE TABLE admin_session (
  id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT       NOT NULL,
  jti           VARCHAR(64)  UNIQUE NOT NULL,   -- JWT ID claim (UUID)
  ip_address    VARCHAR(45)  NULL,
  user_agent    VARCHAR(512) NULL,
  expires_at    TIMESTAMP    NOT NULL,
  revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
  revoked_at    TIMESTAMP    NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (admin_user_id) REFERENCES admin_user(id),
  KEY idx_jti        (jti),
  KEY idx_user       (admin_user_id),
  KEY idx_expires    (expires_at)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- PASSWORD RESET
-- ─────────────────────────────────────────────────────────────────────────────

-- admin_password_reset_token: One-time tokens for the forgot-password flow
-- token_hash = SHA-256(raw_token); raw token is emailed, never stored
CREATE TABLE admin_password_reset_token (
  id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT       NOT NULL,
  token_hash    VARCHAR(256) UNIQUE NOT NULL,
  expires_at    TIMESTAMP    NOT NULL,
  used          BOOLEAN      NOT NULL DEFAULT FALSE,
  used_at       TIMESTAMP    NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (admin_user_id) REFERENCES admin_user(id),
  KEY idx_user (admin_user_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- AUDIT
-- ─────────────────────────────────────────────────────────────────────────────

-- admin_audit_log: Immutable record of every admin action
-- Written after every mutation; never updated or deleted
CREATE TABLE admin_audit_log (
  id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT       NOT NULL,
  action        VARCHAR(128) NOT NULL,    -- e.g. "SUSPEND_CUSTOMER", "FREEZE_WALLET", "RETRY_CALLBACK"
  entity_type   VARCHAR(64)  NULL,        -- e.g. "customer", "wallet", "admin_user"
  entity_id     VARCHAR(128) NULL,        -- PK/externalId of the affected record
  reason        VARCHAR(512) NULL,        -- Captured from confirmation dialog for risky actions
  detail        JSON         NULL,        -- { before: {...}, after: {...} } or extra context
  ip_address    VARCHAR(45)  NULL,
  user_agent    VARCHAR(512) NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_admin_user (admin_user_id),
  KEY idx_action     (action),
  KEY idx_entity     (entity_type, entity_id),
  KEY idx_created    (created_at)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- PERFORMANCE INDEXES ON EXISTING TABLES (admin query patterns)
-- ─────────────────────────────────────────────────────────────────────────────

-- Customer list: status filter + date range
ALTER TABLE customer
  ADD INDEX idx_admin_status_created (status, created_at);

-- Transaction list: status + type + date range; failed list: status + error_code
ALTER TABLE transaction
  ADD INDEX idx_admin_status_type_created (status, type, created_at),
  ADD INDEX idx_admin_error_code          (error_code);

-- Callback: unprocessed alert on dashboard; list by type + processed
ALTER TABLE choice_bank_callback
  ADD INDEX idx_admin_processed_created   (processed, created_at),
  ADD INDEX idx_admin_type_processed      (notification_type, processed);

-- KYC list: status + date
ALTER TABLE kyc_verification
  ADD INDEX idx_admin_status_updated      (status, updated_at);

-- Activity log: cross-customer admin queries
ALTER TABLE activity_log
  ADD INDEX idx_admin_action_created      (action, created_at);

-- ─────────────────────────────────────────────────────────────────────────────
-- SEED DATA
-- ─────────────────────────────────────────────────────────────────────────────

-- Menus (all 16 portal routes)
INSERT INTO admin_menu (name, route, icon, parent_id, sort_order) VALUES
  ('Dashboard',             '/dashboard',             'layout-dashboard', NULL, 1),
  ('Customers',             '/customers',             'users',            NULL, 2),
  ('KYC',                   '/kyc',                   'shield-check',     NULL, 3),
  ('Wallets',               '/wallets',               'wallet',           NULL, 4),
  ('Transactions',          '/transactions',          'arrow-left-right', NULL, 5),
  ('Failed Transactions',   '/transactions/failed',   'alert-circle',     NULL, 6),
  ('Callbacks',             '/callbacks',             'webhook',          NULL, 7),
  ('Reports',               '/reports',               'bar-chart-2',      NULL, 8),
  ('Volume Report',         '/reports/volume',        'trending-up',      8,    1),
  ('KYC Funnel',            '/reports/kyc-funnel',    'filter',           8,    2),
  ('Customer Growth',       '/reports/growth',        'users-round',      8,    3),
  ('Audit Log',             '/audit-log',             'clipboard-list',   NULL, 9),
  ('System Health',         '/system-health',         'activity',         NULL, 10),
  ('Menus',                 '/admin/menus',           'menu',             NULL, 11),
  ('Roles',                 '/admin/roles',           'key-round',        NULL, 12),
  ('Admin Users',           '/admin/users',           'user-cog',         NULL, 13);

-- Permissions (all action codes)
INSERT INTO admin_permission (code, description) VALUES
  ('customer:view',           'View customer list and profiles'),
  ('customer:view_pii',       'View unmasked mobile, ID number, and KYC documents'),
  ('customer:export',         'Export customer data with masking based on PII permission'),
  ('customer:suspend',        'Suspend or reactivate a customer account'),
  ('wallet:view',             'View wallet list and balances'),
  ('wallet:freeze',           'Freeze or unfreeze a wallet'),
  ('transaction:view',        'View all transactions and detail'),
  ('transaction:export',      'Export transactions to CSV'),
  ('callback:view',           'View callback log and raw payloads'),
  ('callback:retry',          'Retry a failed callback'),
  ('kyc:view',                'View KYC list and status'),
  ('kyc:view_documents',      'View KYC document images and rejection reasons'),
  ('report:view',             'Access all report screens'),
  ('audit_log:view',          'View the admin and customer audit logs'),
  ('system:health',           'View system health and service status'),
  ('admin:manage_menus',      'Create, edit, and delete portal menus'),
  ('admin:manage_roles',      'Create, edit, and delete roles and their permissions'),
  ('admin:manage_users',      'Create, edit, suspend, and reset admin users');

-- Roles
INSERT INTO admin_role (name, description) VALUES
  ('SUPER_ADMIN', 'Full portal access — all permissions'),
  ('OPERATIONS',  'Customer, KYC, wallet, transaction, callback operations'),
  ('FINANCE',     'Read-only access to transactions, wallets, and reports'),
  ('SUPPORT',     'Read-only customer and transaction visibility');

-- SUPER_ADMIN: all menus
INSERT INTO admin_role_menu (role_id, menu_id)
  SELECT r.id, m.id FROM admin_role r JOIN admin_menu m WHERE r.name = 'SUPER_ADMIN';

-- OPERATIONS: all menus except admin management (menus/roles/users)
INSERT INTO admin_role_menu (role_id, menu_id)
  SELECT r.id, m.id FROM admin_role r JOIN admin_menu m
  WHERE r.name = 'OPERATIONS' AND m.route NOT IN ('/admin/menus', '/admin/roles', '/admin/users');

-- FINANCE: dashboard, wallets, transactions, failed txns, reports, audit log
INSERT INTO admin_role_menu (role_id, menu_id)
  SELECT r.id, m.id FROM admin_role r JOIN admin_menu m
  WHERE r.name = 'FINANCE'
    AND m.route IN ('/dashboard', '/wallets', '/transactions', '/transactions/failed',
                  '/reports', '/reports/volume', '/reports/kyc-funnel', '/reports/growth',
                  '/audit-log');

-- SUPPORT: dashboard, customers, transactions
INSERT INTO admin_role_menu (role_id, menu_id)
  SELECT r.id, m.id FROM admin_role r JOIN admin_menu m
  WHERE r.name = 'SUPPORT' AND m.route IN ('/dashboard', '/customers', '/transactions', '/transactions/failed');

-- SUPER_ADMIN: all permissions
INSERT INTO admin_role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM admin_role r JOIN admin_permission p WHERE r.name = 'SUPER_ADMIN';

-- OPERATIONS: operational permissions, excluding admin management
INSERT INTO admin_role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM admin_role r JOIN admin_permission p
  WHERE r.name = 'OPERATIONS'
    AND p.code NOT IN ('admin:manage_menus', 'admin:manage_roles', 'admin:manage_users');

-- FINANCE: view customers (masked), view wallets, view+export transactions, view reports, view audit log
INSERT INTO admin_role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM admin_role r JOIN admin_permission p
  WHERE r.name = 'FINANCE'
    AND p.code IN ('customer:view', 'wallet:view', 'transaction:view', 'transaction:export',
                 'kyc:view', 'report:view', 'audit_log:view');

-- SUPPORT: view customers and transactions (masked, no export, no PII)
INSERT INTO admin_role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM admin_role r JOIN admin_permission p
  WHERE r.name = 'SUPPORT' AND p.code IN ('customer:view', 'transaction:view', 'kyc:view');
