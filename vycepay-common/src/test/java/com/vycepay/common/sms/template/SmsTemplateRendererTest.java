package com.vycepay.common.sms.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsTemplateRendererTest {

    @Test
    void render_replacesKnownTokens() {
        String out = SmsTemplateRenderer.render(
                "Code {otp} for {purpose_label}",
                Map.of("otp", "123456", "purpose_label", "verification"));
        assertEquals("Code 123456 for verification", out);
    }

    @Test
    void render_unknownTokenBecomesEmpty() {
        String out = SmsTemplateRenderer.render("Hello {missing}!", Map.of());
        assertEquals("Hello !", out);
    }

    @Test
    void render_truncatesTo640() {
        String body = "x".repeat(700);
        String out = SmsTemplateRenderer.render(body, Map.of());
        assertEquals(640, out.length());
    }

    @Test
    void render_nullBodyIsEmpty() {
        assertEquals("", SmsTemplateRenderer.render(null, Map.of()));
    }
}
