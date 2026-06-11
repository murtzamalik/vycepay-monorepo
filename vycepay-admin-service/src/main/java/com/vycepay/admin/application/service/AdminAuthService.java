package com.vycepay.admin.application.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.vycepay.admin.api.v1.dto.AdminRequests.ForgotPasswordRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.LoginRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.MfaRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.ResetPasswordRequest;
import com.vycepay.admin.config.AdminProperties;
import com.vycepay.admin.security.AdminJwtService;
import com.vycepay.admin.security.AdminPrincipal;
import com.vycepay.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles admin login, MFA hook, session creation, and password reset flows. */
@Service
public class AdminAuthService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;
    private final AdminJwtService jwtService;
    private final AdminSessionService sessionService;
    private final AdminSecurityContext securityContext;
    private final TotpService totpService;
    private final AdminPasswordResetNotifier passwordResetNotifier;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminAuthService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, AdminProperties properties, AdminJwtService jwtService, AdminSessionService sessionService, AdminSecurityContext securityContext, TotpService totpService, AdminPasswordResetNotifier passwordResetNotifier) {
        this.jdbcTemplate = jdbcTemplate; this.passwordEncoder = passwordEncoder; this.properties = properties; this.jwtService = jwtService; this.sessionService = sessionService; this.securityContext = securityContext; this.totpService = totpService; this.passwordResetNotifier = passwordResetNotifier;
    }

    @Transactional
    public Map<String, Object> login(LoginRequest request, HttpServletRequest servletRequest) {
        String username = request.username();
        String password = request.password();
        var users = jdbcTemplate.queryForList("SELECT * FROM admin_user WHERE username=? OR email=?", username, username);
        if (users.isEmpty()) { throw new BusinessException("ADMIN_BAD_CREDENTIALS", "Invalid username or password", HttpStatus.UNAUTHORIZED); }
        var user = users.get(0);
        Long userId = ((Number) user.get("id")).longValue();
        if (!"ACTIVE".equals(user.get("status"))) { throw new BusinessException("ADMIN_DISABLED", "Admin user is not active", HttpStatus.FORBIDDEN); }
        Timestamp lockedUntil = (Timestamp) user.get("locked_until");
        if (lockedUntil != null && lockedUntil.toInstant().isAfter(Instant.now())) { throw new BusinessException("ADMIN_LOCKED", "Admin account is temporarily locked", HttpStatus.LOCKED); }
        if (!passwordEncoder.matches(password, (String) user.get("password_hash"))) {
            int failed = ((Number) user.get("failed_login_attempts")).intValue() + 1;
            Timestamp lock = failed >= properties.getLogin().getMaxAttempts() ? Timestamp.from(Instant.now().plus(properties.getLogin().getLockoutMinutes(), ChronoUnit.MINUTES)) : null;
            jdbcTemplate.update("UPDATE admin_user SET failed_login_attempts=?, locked_until=? WHERE id=?", failed, lock, userId);
            throw new BusinessException("ADMIN_BAD_CREDENTIALS", "Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        jdbcTemplate.update("UPDATE admin_user SET failed_login_attempts=0, locked_until=NULL, last_login_at=CURRENT_TIMESTAMP WHERE id=?", userId);
        boolean mfaEnabled = Boolean.TRUE.equals(user.get("mfa_enabled"));
        Instant expires = Instant.now().plusMillis(jwtService.getExpirationMs());
        String jti = sessionService.createSession(userId, expires, AdminAuditService.clientIp(servletRequest), servletRequest.getHeader("User-Agent"), !mfaEnabled);
        if (mfaEnabled) {
            return Map.of("mfaRequired", true, "jti", jti);
        }
        return authPayload(userId, (String) user.get("external_id"), (String) user.get("username"), jti);
    }

    @Transactional
    public Map<String, Object> verifyMfa(MfaRequest request) {
        String jti = request.jti();
        var rows = jdbcTemplate.queryForList("SELECT u.id, u.external_id, u.username, u.mfa_secret FROM admin_session s JOIN admin_user u ON u.id=s.admin_user_id WHERE s.jti=? AND s.revoked=TRUE AND s.expires_at>CURRENT_TIMESTAMP", jti);
        if (rows.isEmpty()) { throw new BusinessException("ADMIN_SESSION_NOT_FOUND", "MFA session expired", HttpStatus.UNAUTHORIZED); }
        if (!totpService.verify((String) rows.get(0).get("mfa_secret"), request.totpCode())) {
            throw new BusinessException("ADMIN_MFA_INVALID", "Invalid MFA code", HttpStatus.UNAUTHORIZED);
        }
        sessionService.activateSession(jti);
        var row = rows.get(0);
        return authPayload(((Number) row.get("id")).longValue(), (String) row.get("external_id"), (String) row.get("username"), jti);
    }

    public Map<String, Object> me() {
        AdminPrincipal principal = securityContext.currentAdmin();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adminUser", adminUser(principal.id()));
        response.put("roles", principal.roles());
        response.put("permissions", principal.permissions());
        response.put("menus", menuTree(principal.id()));
        return response;
    }

    @Transactional public void logout() { sessionService.revoke(securityContext.currentAdmin().jti()); }

    @Transactional
    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        String email = request.email();
        var rows = jdbcTemplate.queryForList("SELECT id FROM admin_user WHERE email=?", email);
        if (!rows.isEmpty()) {
            Long userId = ((Number) rows.get(0).get("id")).longValue();
            String rawToken = randomToken();
            jdbcTemplate.update("INSERT INTO admin_password_reset_token (admin_user_id, token_hash, expires_at) VALUES (?, ?, ?)", userId, sha256(rawToken), Timestamp.from(Instant.now().plus(30, ChronoUnit.MINUTES)));
            passwordResetNotifier.notifyResetToken(userId, email, rawToken);
        }
        return Map.of("accepted", true, "expiresInMinutes", 30);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.token();
        String newPassword = request.newPassword();
        var rows = jdbcTemplate.queryForList("SELECT * FROM admin_password_reset_token WHERE token_hash=? AND used=FALSE AND expires_at>CURRENT_TIMESTAMP", sha256(token));
        if (rows.isEmpty()) { throw new BusinessException("ADMIN_RESET_INVALID", "Password reset token is invalid or expired", HttpStatus.BAD_REQUEST); }
        var row = rows.get(0);
        Long tokenId = ((Number) row.get("id")).longValue();
        Long userId = ((Number) row.get("admin_user_id")).longValue();
        jdbcTemplate.update("UPDATE admin_user SET password_hash=? WHERE id=?", passwordEncoder.encode(newPassword), userId);
        jdbcTemplate.update("UPDATE admin_password_reset_token SET used=TRUE, used_at=CURRENT_TIMESTAMP WHERE id=?", tokenId);
        jdbcTemplate.update("UPDATE admin_session SET revoked=TRUE, revoked_at=CURRENT_TIMESTAMP WHERE admin_user_id=?", userId);
    }

    private Map<String, Object> authPayload(Long userId, String externalId, String username, String jti) {
        String token = jwtService.createToken(externalId, username, jti);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token); response.put("jti", jti); response.put("adminUser", adminUser(userId)); response.put("menus", menuTree(userId));
        response.put("permissions", jdbcTemplate.queryForList("SELECT DISTINCT p.code FROM admin_user_role ur JOIN admin_role_permission rp ON rp.role_id=ur.role_id JOIN admin_permission p ON p.id=rp.permission_id WHERE ur.user_id=?", String.class, userId));
        return response;
    }
    private Map<String, Object> adminUser(Long userId) { return jdbcTemplate.queryForMap("SELECT id, external_id externalId, username, email, full_name fullName, status, last_login_at lastLoginAt FROM admin_user WHERE id=?", userId); }
    private java.util.List<Map<String, Object>> menuTree(Long userId) { return jdbcTemplate.queryForList("SELECT DISTINCT m.id, m.name, m.route, m.icon, m.parent_id parentId, m.sort_order sortOrder FROM admin_user_role ur JOIN admin_role_menu rm ON rm.role_id=ur.role_id JOIN admin_menu m ON m.id=rm.menu_id WHERE ur.user_id=? ORDER BY COALESCE(m.parent_id, m.id), m.sort_order", userId); }
    private String randomToken() { byte[] bytes = new byte[32]; secureRandom.nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
    private String sha256(String input) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
}
