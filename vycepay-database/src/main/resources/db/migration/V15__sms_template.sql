-- System SMS templates (OTP + money events). One row per template_key; admin edits body only.
CREATE TABLE sms_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_key VARCHAR(64) NOT NULL,
  category VARCHAR(16) NOT NULL COMMENT 'OTP or TRANSACTION',
  name VARCHAR(128) NOT NULL,
  body VARCHAR(640) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  updated_by_admin_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sms_template_key (template_key),
  KEY idx_sms_template_category (category, active)
);

-- Seed = current production copy (behavior-identical until ops edits)
INSERT INTO sms_template (template_key, category, name, body, active) VALUES
('OTP_SIGNUP', 'OTP', 'Signup OTP',
 'Your VycePay verification code is {otp}. Valid for {valid_minutes} minutes. Do not share.', 1),
('OTP_DEVICE_BIND', 'OTP', 'Device bind OTP',
 'Your VycePay new device login code is {otp}. Valid for {valid_minutes} minutes. Do not share.', 1),
('OTP_PIN_RESET', 'OTP', 'PIN reset OTP',
 'Your VycePay PIN reset code is {otp}. Valid for {valid_minutes} minutes. Do not share.', 1),
('OTP_CREDENTIALS_MIGRATE', 'OTP', 'Credentials migrate OTP',
 'Your VycePay account setup code is {otp}. Valid for {valid_minutes} minutes. Do not share.', 1),
('TX_PAY_TILL_SUCCESS', 'TRANSACTION', 'Pay Till success',
 'Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}', 1),
('TX_PAY_BILL_SUCCESS', 'TRANSACTION', 'Pay Bill success',
 'Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}', 1),
('TX_MOBILE_MONEY_SUCCESS', 'TRANSACTION', 'Mobile money send success',
 'Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}', 1),
('TX_TRANSFER_SUCCESS', 'TRANSACTION', 'Transfer success',
 'Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}', 1),
('TX_INBOUND_SUCCESS', 'TRANSACTION', 'Inbound credit success',
 'Received {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}', 1),
('TX_FAILED', 'TRANSACTION', 'Transaction failed',
 'Your transaction of {currency} {amount} failed.{error_suffix} Ref: {ref}', 1),
('TX_DEFAULT_SUCCESS', 'TRANSACTION', 'Default money success',
 'Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}', 1);

-- Admin menu + permission
INSERT INTO admin_permission (code, description)
SELECT 'sms:template:edit', 'Edit system SMS templates'
WHERE NOT EXISTS (SELECT 1 FROM admin_permission WHERE code = 'sms:template:edit');

SET @sms_menu_id = (SELECT id FROM admin_menu WHERE route = '/sms' LIMIT 1);

INSERT INTO admin_menu (name, route, icon, parent_id, sort_order)
SELECT 'SMS templates', '/sms/templates', 'file-text', @sms_menu_id, 3
WHERE @sms_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM admin_menu WHERE route = '/sms/templates');

INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code = 'sms:template:edit'
WHERE r.name IN ('SUPER_ADMIN', 'OPERATIONS')
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM admin_role r
JOIN admin_menu m ON m.route = '/sms/templates'
WHERE r.name IN ('SUPER_ADMIN', 'OPERATIONS', 'SUPPORT')
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );
