-- Admin nav: SMS outbox (money-event retry queue) under SMS menu.
SET @sms_menu_id = (SELECT id FROM admin_menu WHERE route = '/sms' LIMIT 1);

INSERT INTO admin_menu (name, route, icon, parent_id, sort_order)
SELECT 'SMS outbox', '/sms/outbox', 'inbox', @sms_menu_id, 2
WHERE @sms_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM admin_menu WHERE route = '/sms/outbox');

INSERT INTO admin_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM admin_role r
JOIN admin_menu m ON m.route = '/sms/outbox'
WHERE r.name IN ('SUPER_ADMIN', 'OPERATIONS', 'SUPPORT')
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );
