-- customer_notification: in-app inbox source of truth (callback-driven + admin compose)
CREATE TABLE customer_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  public_id CHAR(36) NOT NULL,
  customer_id BIGINT NOT NULL,
  source VARCHAR(32) NOT NULL COMMENT 'CALLBACK or ADMIN_COMPOSE',
  push_type VARCHAR(64) NOT NULL,
  notification_type VARCHAR(8) NULL COMMENT 'Choice notificationType when source=CALLBACK',
  title VARCHAR(128) NOT NULL,
  body VARCHAR(512) NOT NULL,
  data_json JSON NULL,
  choice_callback_id BIGINT NULL,
  batch_id CHAR(36) NULL COMMENT 'Shared across multi-recipient admin compose',
  created_by_admin_id BIGINT NULL,
  read_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_customer_notification_public_id (public_id),
  KEY idx_notif_customer_created (customer_id, deleted_at, created_at),
  KEY idx_notif_batch (batch_id),
  KEY idx_notif_push_type_created (push_type, created_at),
  KEY idx_notif_callback (choice_callback_id),
  CONSTRAINT fk_customer_notification_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- push_delivery_log: one row per FCM send attempt (auto, compose, or resend)
CREATE TABLE push_delivery_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notification_id BIGINT NULL,
  customer_id BIGINT NULL,
  status VARCHAR(32) NOT NULL COMMENT 'SENT, PARTIAL, FAILED, SKIPPED',
  skip_reason VARCHAR(64) NULL,
  token_count INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  failure_count INT NOT NULL DEFAULT 0,
  error_message VARCHAR(255) NULL,
  trigger_source VARCHAR(32) NOT NULL COMMENT 'AUTO, RESEND, COMPOSE',
  created_by_admin_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_push_delivery_notification (notification_id, created_at),
  KEY idx_push_delivery_status_created (status, created_at),
  KEY idx_push_delivery_customer (customer_id, created_at),
  CONSTRAINT fk_push_delivery_notification FOREIGN KEY (notification_id) REFERENCES customer_notification(id)
);

-- Admin menu
INSERT INTO admin_menu (name, route, icon, parent_id, sort_order)
VALUES ('Notifications', '/notifications', 'bell', NULL, 7);

SET @notif_menu_id = LAST_INSERT_ID();

INSERT INTO admin_menu (name, route, icon, parent_id, sort_order)
VALUES ('Notification summary', '/notifications/summary', 'bar-chart-2', @notif_menu_id, 1);

-- Shift later menus' sort_order is not required; 7 places Notifications near Callbacks.

INSERT INTO admin_permission (code, description) VALUES
  ('notification:view',    'View notification inbox and delivery attempts'),
  ('notification:resend',  'Resend an existing notification via FCM'),
  ('notification:compose', 'Compose and send notifications to one or more customers');

-- SUPER_ADMIN already gets all permissions via join-all pattern only at seed time;
-- grant new permissions explicitly to SUPER_ADMIN and OPERATIONS / SUPPORT.
INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code IN ('notification:view', 'notification:resend', 'notification:compose')
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code IN ('notification:view', 'notification:resend', 'notification:compose')
WHERE r.name = 'OPERATIONS'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code = 'notification:view'
WHERE r.name = 'SUPPORT'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM admin_role r
JOIN admin_menu m ON m.route IN ('/notifications', '/notifications/summary')
WHERE r.name IN ('SUPER_ADMIN', 'OPERATIONS', 'SUPPORT')
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );
