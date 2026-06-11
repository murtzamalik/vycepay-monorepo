package com.vycepay.admin.application.service;

import com.vycepay.admin.security.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Persists immutable audit entries for all admin mutations and exports. */
@Service
public class AdminAuditService {
    private final JdbcTemplate jdbcTemplate;
    public AdminAuditService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    public void log(AdminPrincipal admin, String action, String entityType, String entityId, String reason, String detail, HttpServletRequest request) {
        jdbcTemplate.update("INSERT INTO admin_audit_log (admin_user_id, action, entity_type, entity_id, reason, detail, ip_address, user_agent) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", admin.id(), action, entityType, entityId, reason, detail != null ? detail : "{}", clientIp(request), request != null ? request.getHeader("User-Agent") : null);
    }
    public static String clientIp(HttpServletRequest request) {
        if (request == null) { return null; }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) { return forwarded.split(",")[0].trim(); }
        return request.getRemoteAddr();
    }
}
