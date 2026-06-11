package com.vycepay.admin.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vycepay.admin.config.AdminProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AdminJwtServiceTest {
    @Test
    void createsAdminJwtWithJti() {
        AdminProperties properties = new AdminProperties();
        properties.getJwt().setSecret("test-admin-secret-key-minimum-32-bytes");
        properties.getJwt().setExpirationMs(60_000);
        AdminJwtService service = new AdminJwtService(properties, new MockEnvironment());

        String token = service.createToken("admin-ext", "admin", "jti-123");

        var claims = service.parse(token);
        assertNotNull(claims);
        assertEquals("admin-ext", claims.getSubject());
        assertEquals("jti-123", claims.getId());
    }

    @Test
    void rejectsDevelopmentSecretInProdProfile() {
        AdminProperties properties = new AdminProperties();
        properties.getJwt().setSecret("dev-only-admin-secret-key-minimum-32-bytes");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> new AdminJwtService(properties, environment));
    }
}
