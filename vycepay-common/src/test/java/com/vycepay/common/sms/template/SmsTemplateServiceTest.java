package com.vycepay.common.sms.template;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsTemplateServiceTest {

    @Test
    void render_usesActiveDbBodyWhenPresent() {
        SmsTemplatePort port = key -> Optional.of("DB {otp}");
        SmsTemplateService service = new SmsTemplateService(port);
        String out = service.render(SmsTemplateKeys.OTP_SIGNUP, Map.of("otp", "999111"));
        assertEquals("DB 999111", out);
    }

    @Test
    void render_fallsBackToDefaultWhenMissing() {
        SmsTemplatePort port = key -> Optional.empty();
        SmsTemplateService service = new SmsTemplateService(port);
        String out = service.render(SmsTemplateKeys.OTP_PIN_RESET, Map.of(
                "otp", "654321",
                "purpose_label", "PIN reset",
                "valid_minutes", "5"));
        assertTrue(out.contains("654321"));
        assertTrue(out.contains("PIN reset"));
        assertEquals(SmsTemplateRenderer.render(
                SmsTemplateDefaults.bodyFor(SmsTemplateKeys.OTP_PIN_RESET),
                Map.of("otp", "654321", "purpose_label", "PIN reset", "valid_minutes", "5")), out);
    }

    @Test
    void render_fallsBackWhenPortThrows() {
        SmsTemplatePort port = key -> {
            throw new RuntimeException("db down");
        };
        SmsTemplateService service = new SmsTemplateService(port);
        String out = service.render(SmsTemplateKeys.OTP_SIGNUP, Map.of(
                "otp", "111222", "purpose_label", "verification", "valid_minutes", "5"));
        assertTrue(out.contains("111222"));
    }
}
