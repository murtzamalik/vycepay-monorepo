package com.vycepay.admin.application.service;

import java.util.UUID;

import com.vycepay.admin.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Bootstraps the first super admin only when explicit environment variables are provided. */
@Component
public class AdminBootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;
    public AdminBootstrapService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, AdminProperties properties) { this.jdbcTemplate = jdbcTemplate; this.passwordEncoder = passwordEncoder; this.properties = properties; }
    @Override public void run(ApplicationArguments args) {
        AdminProperties.Bootstrap bootstrap = properties.getBootstrap();
        if (isBlank(bootstrap.getUsername()) || isBlank(bootstrap.getEmail()) || isBlank(bootstrap.getPassword())) { return; }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_user", Integer.class);
        if (count != null && count > 0) { return; }
        jdbcTemplate.update("INSERT INTO admin_user (external_id, username, email, password_hash, full_name, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')", UUID.randomUUID().toString(), bootstrap.getUsername(), bootstrap.getEmail(), passwordEncoder.encode(bootstrap.getPassword()), bootstrap.getFullName());
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM admin_user WHERE username=?", Long.class, bootstrap.getUsername());
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM admin_role WHERE name='SUPER_ADMIN'", Long.class);
        jdbcTemplate.update("INSERT INTO admin_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        log.info("Bootstrapped initial SUPER_ADMIN user from environment.");
    }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
