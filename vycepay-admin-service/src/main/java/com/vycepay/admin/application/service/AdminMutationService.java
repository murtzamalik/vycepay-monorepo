package com.vycepay.admin.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vycepay.admin.api.v1.dto.AdminRequests.AdminPasswordResetRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.AdminUserCreateRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.AdminUserUpdateRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.CallbackRetryRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.CustomerStatusRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.MenuRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.NotificationComposeRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.NotificationResendRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.RoleRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.SmsBulkRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.SmsResendRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.SmsTemplatePreviewRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.SmsTemplateUpdateRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.WalletStatusRequest;
import com.vycepay.admin.infrastructure.notification.CallbackNotificationClient;
import com.vycepay.admin.infrastructure.sms.AuthSmsClient;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.sms.KenyaPhoneNormalizer;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import com.vycepay.common.sms.template.SmsTemplateDefaults;
import com.vycepay.common.sms.template.SmsTemplatePlaceholders;
import com.vycepay.common.sms.template.SmsTemplateRenderer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;

/** Performs controlled admin mutations with reason capture and immutable audit entries. */
@Service
public class AdminMutationService {
    private final JdbcTemplate jdbcTemplate;
    private final AdminSecurityContext securityContext;
    private final AdminAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final CallbackNotificationClient notificationClient;
    private final AuthSmsClient authSmsClient;
    private final SmsPort smsPort;
    private final BulkSmsAudienceService bulkSmsAudienceService;
    private final BulkSmsDispatchService bulkSmsDispatchService;
    private final TransactionTemplate transactionTemplate;

    public AdminMutationService(JdbcTemplate jdbcTemplate, AdminSecurityContext securityContext,
                                AdminAuditService auditService, PasswordEncoder passwordEncoder,
                                CallbackNotificationClient notificationClient,
                                AuthSmsClient authSmsClient,
                                SmsPort smsPort,
                                BulkSmsAudienceService bulkSmsAudienceService,
                                BulkSmsDispatchService bulkSmsDispatchService,
                                PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.securityContext = securityContext;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.notificationClient = notificationClient;
        this.authSmsClient = authSmsClient;
        this.smsPort = smsPort;
        this.bulkSmsAudienceService = bulkSmsAudienceService;
        this.bulkSmsDispatchService = bulkSmsDispatchService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional public void updateCustomerStatus(String id, CustomerStatusRequest body, HttpServletRequest req){ Long pk=customerPk(id); jdbcTemplate.update("UPDATE customer SET status=? WHERE id=?", body.status(), pk); auditService.log(securityContext.currentAdmin(), body.status()+"_CUSTOMER", "customer", String.valueOf(pk), body.reason(), "{\"status\":\""+body.status()+"\"}", req); }
    @Transactional public void updateWalletStatus(Long id, WalletStatusRequest body, HttpServletRequest req){ int rows=jdbcTemplate.update("UPDATE wallet SET status=? WHERE id=?", body.status(), id); if(rows==0) throw notFound("WALLET_NOT_FOUND"); auditService.log(securityContext.currentAdmin(), body.status()+"_WALLET", "wallet", String.valueOf(id), body.reason(), "{\"status\":\""+body.status()+"\"}", req); }
    @Transactional public void retryCallback(Long id, CallbackRetryRequest body, HttpServletRequest req){ int rows=jdbcTemplate.update("UPDATE choice_bank_callback SET processed=FALSE, processed_at=NULL, processing_error=NULL WHERE id=?", id); if(rows==0 && jdbcTemplate.queryForObject("SELECT COUNT(*) FROM choice_bank_callback WHERE id=?", Long.class, id)==0) throw notFound("CALLBACK_NOT_FOUND"); auditService.log(securityContext.currentAdmin(), "RETRY_CALLBACK", "choice_bank_callback", String.valueOf(id), body.reason(), "{\"queued\":true}", req); }

    public Map<String, Object> resendNotification(Long id, NotificationResendRequest body, HttpServletRequest req) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_notification WHERE id=? AND deleted_at IS NULL", Long.class, id);
        if (count == null || count == 0) throw notFound("NOTIFICATION_NOT_FOUND");
        Map<String, Object> result = notificationClient.resend(id, securityContext.currentAdmin().id());
        auditService.log(securityContext.currentAdmin(), "RESEND_NOTIFICATION", "customer_notification",
                String.valueOf(id), body.reason(), "{\"status\":\"" + result.get("status") + "\"}", req);
        return result;
    }

    public Map<String, Object> composeNotification(NotificationComposeRequest body, HttpServletRequest req) {
        Map<String, Object> result = notificationClient.compose(
                body.customerIds(), body.title(), body.body(), body.data(), securityContext.currentAdmin().id());
        auditService.log(securityContext.currentAdmin(), "COMPOSE_NOTIFICATION", "customer_notification",
                String.valueOf(result.get("batchId")), body.reason(),
                "{\"accepted\":" + result.get("accepted") + ",\"recipients\":" + body.customerIds().size() + "}", req);
        return result;
    }

    /**
     * Resends SMS: AUTH_OTP via auth (new code); ADMIN_BULK re-sends same body.
     */
    public Map<String, Object> resendSms(Long id, SmsResendRequest body, HttpServletRequest req) {
        var rows = jdbcTemplate.queryForList(
                "SELECT id, purpose, recipient, message_body messageBody FROM sms_message WHERE id=?", id);
        if (rows.isEmpty()) {
            throw notFound("SMS_NOT_FOUND");
        }
        String purpose = (String) rows.get(0).get("purpose");
        Long adminId = securityContext.currentAdmin().id();
        Map<String, Object> result;
        if ("AUTH_OTP".equals(purpose)) {
            result = authSmsClient.resendAuthOtp(id, adminId);
        } else if ("ADMIN_BULK".equals(purpose)) {
            result = resendBulkRow(id, (String) rows.get(0).get("recipient"),
                    (String) rows.get(0).get("messageBody"), adminId);
        } else {
            throw new BusinessException("SMS_UNSUPPORTED_PURPOSE",
                    "Cannot resend SMS with purpose " + purpose, HttpStatus.BAD_REQUEST);
        }
        auditService.log(securityContext.currentAdmin(), "RESEND_SMS", "sms_message",
                String.valueOf(id), body.reason(),
                "{\"purpose\":\"" + purpose + "\",\"status\":\"" + result.get("status") + "\"}", req);
        return result;
    }

    /**
     * Re-queues a money-event SMS outbox row for the callback retry job (PENDING, next_attempt_at=now).
     * SENT rows cannot be requeued.
     */
    @Transactional
    public Map<String, Object> retrySmsOutbox(Long id, SmsResendRequest body, HttpServletRequest req) {
        var rows = jdbcTemplate.queryForList(
                "SELECT id, status, dedupe_key dedupeKey FROM sms_outbox WHERE id=?", id);
        if (rows.isEmpty()) {
            throw notFound("SMS_OUTBOX_NOT_FOUND");
        }
        String status = (String) rows.get(0).get("status");
        if ("SENT".equals(status)) {
            throw new BusinessException("SMS_OUTBOX_ALREADY_SENT",
                    "Outbox row already SENT; cannot requeue", HttpStatus.BAD_REQUEST);
        }
        int updated = jdbcTemplate.update(
                "UPDATE sms_outbox SET status='PENDING', next_attempt_at=CURRENT_TIMESTAMP, "
                        + "last_error=NULL, updated_at=CURRENT_TIMESTAMP WHERE id=? AND status<>'SENT'",
                id);
        if (updated == 0) {
            throw notFound("SMS_OUTBOX_NOT_FOUND");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("status", "PENDING");
        data.put("dedupeKey", rows.get(0).get("dedupeKey"));
        data.put("queued", true);
        auditService.log(securityContext.currentAdmin(), "RETRY_SMS_OUTBOX", "sms_outbox",
                String.valueOf(id), body.reason(),
                "{\"previousStatus\":\"" + status + "\",\"queued\":true}", req);
        return data;
    }

    /**
     * Updates name/body/active for a seeded SMS template key (no create/delete).
     */
    @Transactional
    public Map<String, Object> updateSmsTemplate(String templateKey, SmsTemplateUpdateRequest body,
                                                 HttpServletRequest req) {
        if (templateKey == null || templateKey.isBlank() || !SmsTemplateDefaults.isKnownKey(templateKey.trim())) {
            throw new BusinessException("SMS_TEMPLATE_UNKNOWN_KEY",
                    "Unknown SMS template key", HttpStatus.BAD_REQUEST);
        }
        String key = templateKey.trim();
        Long adminId = securityContext.currentAdmin().id();
        int rows = jdbcTemplate.update(
                "UPDATE sms_template SET name=?, body=?, active=?, updated_by_admin_id=?, "
                        + "updated_at=CURRENT_TIMESTAMP WHERE template_key=?",
                body.name().trim(), body.body().trim(), Boolean.TRUE.equals(body.active()) ? 1 : 0,
                adminId, key);
        if (rows == 0) {
            throw notFound("SMS_TEMPLATE_NOT_FOUND");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateKey", key);
        data.put("name", body.name().trim());
        data.put("body", body.body().trim());
        data.put("active", Boolean.TRUE.equals(body.active()));
        auditService.log(securityContext.currentAdmin(), "UPDATE_SMS_TEMPLATE", "sms_template", key,
                body.reason(),
                "{\"active\":" + Boolean.TRUE.equals(body.active()) + "}", req);
        return data;
    }

    /**
     * Renders a template body with sample or provided vars (no send).
     */
    public Map<String, Object> previewSmsTemplate(String templateKey, SmsTemplatePreviewRequest body) {
        if (templateKey == null || templateKey.isBlank() || !SmsTemplateDefaults.isKnownKey(templateKey.trim())) {
            throw new BusinessException("SMS_TEMPLATE_UNKNOWN_KEY",
                    "Unknown SMS template key", HttpStatus.BAD_REQUEST);
        }
        String key = templateKey.trim();
        var rows = jdbcTemplate.queryForList(
                "SELECT category FROM sms_template WHERE template_key=?", key);
        String category = rows.isEmpty() ? null : (String) rows.get(0).get("category");
        if (category == null) {
            category = key.startsWith("OTP_") ? "OTP" : "TRANSACTION";
        }
        Map<String, String> vars = body.vars() != null && !body.vars().isEmpty()
                ? body.vars()
                : SmsTemplatePlaceholders.sampleVars(category);
        String rendered = SmsTemplateRenderer.render(body.body(), vars);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateKey", key);
        data.put("rendered", rendered);
        data.put("length", rendered != null ? rendered.length() : 0);
        data.put("vars", vars);
        return data;
    }

    /**
     * Preview counts for ALL_CUSTOMERS audience (eligible statuses, valid Kenya mobiles).
     */
    public Map<String, Object> bulkSmsAudiencePreview() {
        return bulkSmsAudienceService.preview();
    }

    /**
     * Bulk SMS: MANUAL phone list (max 100, sync) or ALL_CUSTOMERS (enqueue + async send).
     */
    public Map<String, Object> bulkSms(SmsBulkRequest body, HttpServletRequest req) {
        if ("ALL_CUSTOMERS".equals(body.resolvedAudience())) {
            return bulkSmsAllCustomers(body, req);
        }
        return bulkSmsManual(body, req);
    }

    private Map<String, Object> bulkSmsManual(SmsBulkRequest body, HttpServletRequest req) {
        List<String> normalized = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        for (String raw : body.recipients()) {
            KenyaPhoneNormalizer.toRecipient(raw).ifPresentOrElse(normalized::add, () -> invalid.add(raw));
        }
        if (!invalid.isEmpty()) {
            throw new BusinessException("SMS_INVALID_RECIPIENTS",
                    "Invalid phone numbers: " + String.join(", ", invalid.stream().limit(5).toList()),
                    HttpStatus.BAD_REQUEST);
        }
        if (normalized.isEmpty()) {
            throw new BusinessException("SMS_NO_RECIPIENTS", "At least one valid recipient is required",
                    HttpStatus.BAD_REQUEST);
        }

        String batchId = UUID.randomUUID().toString();
        Long adminId = securityContext.currentAdmin().id();
        String message = body.message().trim();
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        List<Long> ids = new ArrayList<>();

        for (String recipient : normalized) {
            Long smsId = insertBulkSmsRow(batchId, recipient, null, message, adminId);
            ids.add(smsId);

            SmsSendResult result = smsPort.send(new SmsSendRequest(recipient, message));
            bulkSmsDispatchService.applyBulkResult(smsId, result, "BULK", adminId);
            if (SmsSendResult.SENT.equals(result.status())) {
                sent++;
            } else if (SmsSendResult.SKIPPED.equals(result.status())) {
                skipped++;
            } else {
                failed++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("batchId", batchId);
        data.put("total", normalized.size());
        data.put("sent", sent);
        data.put("failed", failed);
        data.put("skipped", skipped);
        data.put("smsMessageIds", ids);
        data.put("audience", "MANUAL");
        data.put("status", "COMPLETED");

        auditService.log(securityContext.currentAdmin(), "SMS_BULK", "sms_message", batchId, body.reason(),
                "{\"audience\":\"MANUAL\",\"total\":" + normalized.size()
                        + ",\"sent\":" + sent + ",\"failed\":" + failed + "}", req);
        return data;
    }

    private Map<String, Object> bulkSmsAllCustomers(SmsBulkRequest body, HttpServletRequest req) {
        BulkSmsAudienceService.ResolvedAudience resolved = bulkSmsAudienceService.resolve();
        List<BulkSmsAudienceService.AudienceRecipient> recipients = resolved.recipients();
        if (recipients.isEmpty()) {
            throw new BusinessException("SMS_NO_RECIPIENTS",
                    "No eligible customers with a valid Kenya mobile", HttpStatus.BAD_REQUEST);
        }
        if (recipients.size() > BulkSmsAudienceService.MAX_ALL_CUSTOMERS) {
            throw new BusinessException("SMS_AUDIENCE_TOO_LARGE",
                    "Audience exceeds maximum of " + BulkSmsAudienceService.MAX_ALL_CUSTOMERS
                            + " recipients (" + recipients.size() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        String batchId = UUID.randomUUID().toString();
        Long adminId = securityContext.currentAdmin().id();
        String message = body.message().trim();
        List<Long> ids = transactionTemplate.execute(status -> {
            List<Long> inserted = new ArrayList<>(recipients.size());
            for (BulkSmsAudienceService.AudienceRecipient target : recipients) {
                inserted.add(insertBulkSmsRow(batchId, target.recipient(), target.customerId(), message, adminId));
            }
            return inserted;
        });
        if (ids == null) {
            ids = List.of();
        }

        // Dispatch only after inserts commit so the async worker can see PENDING rows.
        bulkSmsDispatchService.dispatchBatch(batchId, adminId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("batchId", batchId);
        data.put("total", recipients.size());
        data.put("sent", 0);
        data.put("failed", 0);
        data.put("skipped", resolved.skippedInvalid());
        data.put("smsMessageIds", ids);
        data.put("audience", "ALL_CUSTOMERS");
        data.put("status", "ACCEPTED");

        auditService.log(securityContext.currentAdmin(), "SMS_BULK", "sms_message", batchId, body.reason(),
                "{\"audience\":\"ALL_CUSTOMERS\",\"total\":" + recipients.size()
                        + ",\"skippedInvalid\":" + resolved.skippedInvalid() + ",\"status\":\"ACCEPTED\"}",
                req);
        return data;
    }


    /**
     * Inserts an ADMIN_BULK sms_message row and returns the generated PK (same connection).
     */
    private Long insertBulkSmsRow(String batchId, String recipient, Long customerId, String message, Long adminId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String publicId = UUID.randomUUID().toString();
        jdbcTemplate.update(connection -> {
            boolean withCustomer = customerId != null;
            String sql = withCustomer
                    ? "INSERT INTO sms_message (public_id, batch_id, recipient, customer_id, purpose, message_body, "
                    + "message_redacted, provider, status, created_by_admin_id) "
                    + "VALUES (?, ?, ?, ?, 'ADMIN_BULK', ?, ?, 'MOBIWAVE', 'PENDING', ?)"
                    : "INSERT INTO sms_message (public_id, batch_id, recipient, purpose, message_body, message_redacted, "
                    + "provider, status, created_by_admin_id) VALUES (?, ?, ?, 'ADMIN_BULK', ?, ?, 'MOBIWAVE', 'PENDING', ?)";
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, publicId);
            ps.setString(i++, batchId);
            ps.setString(i++, recipient);
            if (withCustomer) {
                ps.setLong(i++, customerId);
            }
            ps.setString(i++, message);
            ps.setString(i++, message);
            ps.setLong(i, adminId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BusinessException("SMS_INSERT_FAILED", "Failed to persist SMS message", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return key.longValue();
    }

    private Map<String, Object> resendBulkRow(Long id, String recipient, String messageBody, Long adminId) {
        if (recipient == null || messageBody == null) {
            throw new BusinessException("SMS_INVALID", "Missing recipient or body for resend", HttpStatus.BAD_REQUEST);
        }
        SmsSendResult result = smsPort.send(new SmsSendRequest(recipient, messageBody));
        bulkSmsDispatchService.applyBulkResult(id, result, "RESEND", adminId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("smsMessageId", id);
        data.put("status", result.status());
        data.put("providerUid", result.providerUid());
        data.put("errorMessage", result.errorMessage());
        return data;
    }

    @Transactional public Long createMenu(MenuRequest body, HttpServletRequest req){ jdbcTemplate.update("INSERT INTO admin_menu (name, route, icon, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", body.name(), body.route(), body.icon(), body.parentId(), intVal(body.sortOrder())); Long id=jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class); auditService.log(securityContext.currentAdmin(), "CREATE_MENU", "admin_menu", String.valueOf(id), null, "{}", req); return id; }
    @Transactional public void updateMenu(Long id, MenuRequest body, HttpServletRequest req){ jdbcTemplate.update("UPDATE admin_menu SET name=?, route=?, icon=?, parent_id=?, sort_order=? WHERE id=?", body.name(), body.route(), body.icon(), body.parentId(), intVal(body.sortOrder()), id); auditService.log(securityContext.currentAdmin(), "UPDATE_MENU", "admin_menu", String.valueOf(id), null, "{}", req); }
    @Transactional public void deleteMenu(Long id, HttpServletRequest req){ Long count=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_role_menu WHERE menu_id=?", Long.class, id); if(count!=null&&count>0) throw new BusinessException("MENU_ASSIGNED", "Cannot delete a menu assigned to roles", HttpStatus.CONFLICT); jdbcTemplate.update("DELETE FROM admin_menu WHERE id=?", id); auditService.log(securityContext.currentAdmin(), "DELETE_MENU", "admin_menu", String.valueOf(id), null, "{}", req); }
    @Transactional public Long createRole(RoleRequest body, HttpServletRequest req){ jdbcTemplate.update("INSERT INTO admin_role (name, description) VALUES (?, ?)", body.name(), body.description()); Long id=jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class); replaceRoleAssignments(id, body.menuIds(), body.permissionCodes()); auditService.log(securityContext.currentAdmin(), "CREATE_ROLE", "admin_role", String.valueOf(id), body.reason(), "{}", req); return id; }
    @Transactional public void updateRole(Long id, RoleRequest body, HttpServletRequest req){ jdbcTemplate.update("UPDATE admin_role SET name=?, description=? WHERE id=?", body.name(), body.description(), id); replaceRoleAssignments(id, body.menuIds(), body.permissionCodes()); auditService.log(securityContext.currentAdmin(), "UPDATE_ROLE", "admin_role", String.valueOf(id), body.reason(), "{}", req); }
    @Transactional public void deleteRole(Long id, HttpServletRequest req){ Long count=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_user_role WHERE role_id=?", Long.class, id); if(count!=null&&count>0) throw new BusinessException("ROLE_ASSIGNED", "Cannot delete a role assigned to users", HttpStatus.CONFLICT); jdbcTemplate.update("DELETE FROM admin_role WHERE id=?", id); auditService.log(securityContext.currentAdmin(), "DELETE_ROLE", "admin_role", String.valueOf(id), null, "{}", req); }
    @Transactional public Long createAdminUser(AdminUserCreateRequest body, HttpServletRequest req){ jdbcTemplate.update("INSERT INTO admin_user (external_id, username, email, full_name, password_hash, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')", UUID.randomUUID().toString(), body.username(), body.email(), body.fullName(), passwordEncoder.encode(body.password())); Long id=jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class); replaceUserRoles(id, body.roleIds()); auditService.log(securityContext.currentAdmin(), "CREATE_ADMIN_USER", "admin_user", String.valueOf(id), body.reason(), "{}", req); return id; }
    @Transactional public void updateAdminUser(Long id, AdminUserUpdateRequest body, HttpServletRequest req){ jdbcTemplate.update("UPDATE admin_user SET email=?, full_name=?, status=? WHERE id=?", body.email(), body.fullName(), body.status(), id); replaceUserRoles(id, body.roleIds()); auditService.log(securityContext.currentAdmin(), "UPDATE_ADMIN_USER", "admin_user", String.valueOf(id), body.reason(), "{}", req); }
    @Transactional public void resetAdminPassword(Long id, AdminPasswordResetRequest body, HttpServletRequest req){ jdbcTemplate.update("UPDATE admin_user SET password_hash=? WHERE id=?", passwordEncoder.encode(body.newPassword()), id); jdbcTemplate.update("UPDATE admin_session SET revoked=TRUE, revoked_at=CURRENT_TIMESTAMP WHERE admin_user_id=?", id); auditService.log(securityContext.currentAdmin(), "RESET_ADMIN_PASSWORD", "admin_user", String.valueOf(id), body.reason(), "{}", req); }
    private void replaceRoleAssignments(Long roleId, List<Long> menuIds, List<String> permissionCodes){ jdbcTemplate.update("DELETE FROM admin_role_menu WHERE role_id=?", roleId); jdbcTemplate.update("DELETE FROM admin_role_permission WHERE role_id=?", roleId); for(Long menuId: menuIds) jdbcTemplate.update("INSERT INTO admin_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId); for(String code: permissionCodes) jdbcTemplate.update("INSERT INTO admin_role_permission (role_id, permission_id) SELECT ?, id FROM admin_permission WHERE code=?", roleId, code); }
    private void replaceUserRoles(Long userId, List<Long> roleIds){ jdbcTemplate.update("DELETE FROM admin_user_role WHERE user_id=?", userId); for(Long roleId: roleIds) jdbcTemplate.update("INSERT INTO admin_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId); }
    private Long customerPk(String id){ var rows=jdbcTemplate.queryForList("SELECT id FROM customer WHERE external_id=? OR id=?", id, numericId(id)); if(rows.isEmpty()) throw notFound("CUSTOMER_NOT_FOUND"); return ((Number)rows.get(0).get("id")).longValue(); }
    private BusinessException notFound(String code){ return new BusinessException(code, "Resource not found", HttpStatus.NOT_FOUND); }
    private Long numericId(String id){ try{return Long.parseLong(id);}catch(Exception e){return -1L;} } private int intVal(Object v){ return v instanceof Number n?n.intValue():v==null?0:Integer.parseInt(String.valueOf(v)); }
}
