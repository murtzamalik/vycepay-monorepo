package com.vycepay.common.config;

import com.vycepay.common.sms.template.JdbcSmsTemplateAdapter;
import com.vycepay.common.sms.template.SmsTemplatePort;
import com.vycepay.common.sms.template.SmsTemplateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wires SMS template load/render when a {@link JdbcTemplate} is available.
 */
@Configuration
public class SmsTemplateConfig {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean(SmsTemplatePort.class)
    public SmsTemplatePort smsTemplatePort(JdbcTemplate jdbcTemplate) {
        return new JdbcSmsTemplateAdapter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnBean(SmsTemplatePort.class)
    @ConditionalOnMissingBean(SmsTemplateService.class)
    public SmsTemplateService smsTemplateService(SmsTemplatePort smsTemplatePort) {
        return new SmsTemplateService(smsTemplatePort);
    }
}
