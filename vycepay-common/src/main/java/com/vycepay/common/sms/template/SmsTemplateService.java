package com.vycepay.common.sms.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Resolves active DB template (or code default) and renders placeholders.
 */
public class SmsTemplateService {

    private static final Logger log = LoggerFactory.getLogger(SmsTemplateService.class);

    private final SmsTemplatePort templatePort;

    public SmsTemplateService(SmsTemplatePort templatePort) {
        this.templatePort = templatePort;
    }

    /**
     * Active DB body if present; else {@link SmsTemplateDefaults}; never returns blank for known keys.
     */
    public String render(String templateKey, Map<String, String> vars) {
        String body = null;
        try {
            if (templatePort != null) {
                body = templatePort.findActiveBody(templateKey).orElse(null);
            }
        } catch (Exception e) {
            log.warn("SMS template load failed key={}: {}", templateKey, e.getMessage());
        }
        if (body == null || body.isBlank()) {
            body = SmsTemplateDefaults.bodyFor(templateKey);
        }
        String rendered = SmsTemplateRenderer.render(body, vars);
        if (rendered == null || rendered.isBlank()) {
            return SmsTemplateDefaults.bodyFor(templateKey);
        }
        return rendered;
    }
}
