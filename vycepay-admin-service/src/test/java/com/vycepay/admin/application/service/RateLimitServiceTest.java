package com.vycepay.admin.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vycepay.admin.config.AdminProperties;
import com.vycepay.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitServiceTest {
    @Test
    void rejectsRequestsOverConfiguredLimit() {
        AdminProperties properties = new AdminProperties();
        properties.getRateLimit().getLogin().setLimit(1);
        properties.getRateLimit().getLogin().setWindowSeconds(60);
        RateLimitService service = new RateLimitService(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7");

        assertDoesNotThrow(() -> service.check("login", request));
        assertThrows(BusinessException.class, () -> service.check("login", request));
    }
}
