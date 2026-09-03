-- sms_message: outbound SMS ledger (auth OTP + admin bulk)
CREATE TABLE sms_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  public_id CHAR(36) NOT NULL,
  batch_id CHAR(36) NULL COMMENT 'Shared across multi-recipient admin bulk',
  recipient VARCHAR(20) NOT NULL COMMENT 'E.164 digits without plus, e.g. 254712345678',
  customer_id BIGINT NULL,
  purpose VARCHAR(32) NOT NULL COMMENT 'AUTH_OTP or ADMIN_BULK',
  otp_purpose VARCHAR(32) NULL COMMENT 'SIGNUP, DEVICE_BIND, PIN_RESET, CREDENTIALS_MIGRATE',
  otp_verification_id BIGINT NULL,
  message_body VARCHAR(640) NOT NULL,
  message_redacted VARCHAR(640) NOT NULL,
  provider VARCHAR(32) NOT NULL DEFAULT 'MOBIWAVE',
  provider_uid VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL COMMENT 'PENDING, SENT, FAILED, SKIPPED',
  error_message VARCHAR(255) NULL,
  created_by_admin_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at TIMESTAMP NULL,
  UNIQUE KEY uk_sms_message_public_id (public_id),
  KEY idx_sms_status_created (status, created_at),
  KEY idx_sms_recipient_created (recipient, created_at),
  KEY idx_sms_batch (batch_id),
  KEY idx_sms_purpose_created (purpose, created_at),
  CONSTRAINT fk_sms_message_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- sms_delivery_attempt: one row per provider send try
CREATE TABLE sms_delivery_attempt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sms_message_id BIGINT NOT NULL,
  trigger_source VARCHAR(32) NOT NULL COMMENT 'AUTO, RESEND, BULK',
  status VARCHAR(32) NOT NULL COMMENT 'SENT, FAILED, SKIPPED',
  provider_uid VARCHAR(64) NULL,
  error_message VARCHAR(255) NULL,
  created_by_admin_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sms_attempt_message (sms_message_id, created_at),
  KEY idx_sms_attempt_status_created (status, created_at),
  CONSTRAINT fk_sms_attempt_message FOREIGN KEY (sms_message_id) REFERENCES sms_message(id)
);

-- Admin menu
INSERT INTO admin_menu (name, route, icon, parent_id, sort_order)
VALUES ('SMS', '/sms', 'message-square', NULL, 8);

SET @sms_menu_id = LAST_INSERT_ID();

INSERT INTO admin_menu (name, route, icon, parent_id, sort_order)
VALUES ('Bulk SMS', '/sms/bulk', 'send', @sms_menu_id, 1);

INSERT INTO admin_permission (code, description) VALUES
  ('sms:view',   'View SMS ledger and delivery attempts'),
  ('sms:resend', 'Resend a failed or existing SMS'),
  ('sms:bulk',   'Compose and send bulk SMS to phone lists');

INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code IN ('sms:view', 'sms:resend', 'sms:bulk')
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code IN ('sms:view', 'sms:resend', 'sms:bulk')
WHERE r.name = 'OPERATIONS'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code = 'sms:view'
WHERE r.name = 'SUPPORT'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM admin_role r
JOIN admin_menu m ON m.route IN ('/sms', '/sms/bulk')
WHERE r.name IN ('SUPER_ADMIN', 'OPERATIONS', 'SUPPORT')
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );
