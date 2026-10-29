-- Mobile app release policy (min/latest version, maintenance). One row per platform.
-- Admin portal edits; auth-service reads for GET /api/v1/auth/app-config.
CREATE TABLE app_release_policy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform VARCHAR(16) NOT NULL COMMENT 'ANDROID or IOS',
  min_supported_version_code INT NOT NULL,
  latest_version_code INT NOT NULL,
  latest_version_name VARCHAR(32) NOT NULL,
  update_title VARCHAR(128) NOT NULL,
  update_message VARCHAR(512) NOT NULL,
  store_url VARCHAR(512) NULL,
  apk_url VARCHAR(512) NULL,
  maintenance TINYINT(1) NOT NULL DEFAULT 0,
  maintenance_message VARCHAR(512) NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  updated_by_admin_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_app_release_platform (platform)
);

-- Safe seed: matches current Drive versionCode=1 (no force until ops raises min)
INSERT INTO app_release_policy (
  platform, min_supported_version_code, latest_version_code, latest_version_name,
  update_title, update_message, store_url, apk_url, maintenance, maintenance_message, active
) VALUES
('ANDROID', 1, 1, '1.0.0',
 'Update available', 'Please update VycePay to continue.',
 'https://play.google.com/store/apps/details?id=com.wallet.vycepay', NULL, 0, NULL, 1),
('IOS', 1, 1, '1.0.0',
 'Update available', 'Please update VycePay to continue.',
 NULL, NULL, 0, NULL, 1);

INSERT INTO admin_permission (code, description)
SELECT 'mobile:release:view', 'View mobile app release policy'
WHERE NOT EXISTS (SELECT 1 FROM admin_permission WHERE code = 'mobile:release:view');

INSERT INTO admin_permission (code, description)
SELECT 'mobile:release:edit', 'Edit mobile app release policy'
WHERE NOT EXISTS (SELECT 1 FROM admin_permission WHERE code = 'mobile:release:edit');

INSERT INTO admin_menu (name, route, icon, parent_id, sort_order)
SELECT 'App release', '/system/app-release', 'smartphone', NULL, 11
WHERE NOT EXISTS (SELECT 1 FROM admin_menu WHERE route = '/system/app-release');

INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code IN ('mobile:release:view', 'mobile:release:edit')
WHERE r.name IN ('SUPER_ADMIN', 'OPERATIONS')
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r
JOIN admin_permission p ON p.code = 'mobile:release:view'
WHERE r.name = 'SUPPORT'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO admin_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM admin_role r
JOIN admin_menu m ON m.route = '/system/app-release'
WHERE r.name IN ('SUPER_ADMIN', 'OPERATIONS', 'SUPPORT')
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );
