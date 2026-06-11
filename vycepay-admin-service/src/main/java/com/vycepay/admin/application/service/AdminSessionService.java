package com.vycepay.admin.application.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.vycepay.admin.security.AdminPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, validates, and revokes server-side admin sessions keyed by JWT jti. */
@Service
public class AdminSessionService {
    private final JdbcTemplate jdbcTemplate;
    public AdminSessionService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Transactional
    public String createSession(Long adminUserId, Instant expiresAt, String ipAddress, String userAgent, boolean active) {
        String jti = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO admin_session (admin_user_id, jti, ip_address, user_agent, expires_at, revoked) VALUES (?, ?, ?, ?, ?, ?)", adminUserId, jti, ipAddress, userAgent, Timestamp.from(expiresAt), !active);
        return jti;
    }
    @Transactional public void activateSession(String jti) { jdbcTemplate.update("UPDATE admin_session SET revoked=FALSE, revoked_at=NULL WHERE jti=?", jti); }
    @Transactional public void revoke(String jti) { jdbcTemplate.update("UPDATE admin_session SET revoked=TRUE, revoked_at=CURRENT_TIMESTAMP WHERE jti=?", jti); }
    public Optional<AdminPrincipal> loadPrincipal(String jti) {
        var sessions = jdbcTemplate.queryForList("SELECT s.admin_user_id, u.external_id, u.username, u.email, u.full_name FROM admin_session s JOIN admin_user u ON u.id = s.admin_user_id WHERE s.jti = ? AND s.revoked = FALSE AND s.expires_at > CURRENT_TIMESTAMP AND u.status = 'ACTIVE'", jti);
        if (sessions.isEmpty()) { return Optional.empty(); }
        var row = sessions.get(0);
        Long adminUserId = ((Number) row.get("admin_user_id")).longValue();
        Set<String> roles = new HashSet<>(jdbcTemplate.queryForList("SELECT r.name FROM admin_user_role ur JOIN admin_role r ON r.id = ur.role_id WHERE ur.user_id = ?", String.class, adminUserId));
        Set<String> permissions = new HashSet<>(jdbcTemplate.queryForList("SELECT DISTINCT p.code FROM admin_user_role ur JOIN admin_role_permission rp ON rp.role_id = ur.role_id JOIN admin_permission p ON p.id = rp.permission_id WHERE ur.user_id = ?", String.class, adminUserId));
        return Optional.of(new AdminPrincipal(adminUserId, (String) row.get("external_id"), (String) row.get("username"), (String) row.get("email"), (String) row.get("full_name"), roles, permissions, jti));
    }
}
