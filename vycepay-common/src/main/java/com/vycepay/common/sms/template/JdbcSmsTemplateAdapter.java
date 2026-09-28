package com.vycepay.common.sms.template;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Reads active {@code sms_template.body} by key.
 */
public class JdbcSmsTemplateAdapter implements SmsTemplatePort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSmsTemplateAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findActiveBody(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return Optional.empty();
        }
        List<String> rows = jdbcTemplate.query(
                "SELECT body FROM sms_template WHERE template_key=? AND active=1 LIMIT 1",
                (rs, i) -> rs.getString(1),
                templateKey.trim());
        if (rows.isEmpty() || rows.get(0) == null || rows.get(0).isBlank()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }
}
