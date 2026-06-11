package com.vycepay.admin.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PiiMaskingServiceTest {
    private final PiiMaskingService service = new PiiMaskingService();

    @Test
    void masksMobileWhenAdminDoesNotHavePiiPermission() {
        assertEquals("+254 *** *** 89", service.maskMobile("+254", "712345689", false));
    }

    @Test
    void leavesMobileVisibleWhenAdminHasPiiPermission() {
        assertEquals("+254712345689", service.maskMobile("+254", "712345689", true));
    }

    @Test
    void masksIdNumberByDefault() {
        assertEquals("*******", service.maskIdNumber("12345678", false));
    }

    @Test
    void masksEmailWhenAdminDoesNotHavePiiPermission() {
        assertEquals("a***@vycepay.com", service.maskEmail("admin@vycepay.com", false));
    }
}
