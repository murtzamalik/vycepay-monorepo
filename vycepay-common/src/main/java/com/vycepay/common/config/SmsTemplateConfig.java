package com.vycepay.common.config;

import com.vycepay.common.sms.template.JdbcSmsTemplateAdapter;
import com.vycepay.common.sms.template.SmsTemplatePort;
import com.vycepay.common.sms.template.SmsTemplateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wires SMS template load/render for services that have JDBC on the classpath.
 * <p>
 * Do not use {@code @ConditionalOnBean(JdbcTemplate)} here: this config is
 * {@code @Import}ed by auth/callback and is processed before JDBC auto-config,
 * so that condition always fails and {@link SmsTemplateService} never registers.
 * Constructor injection of {@link JdbcTemplate} waits until the DataSource beans exist.
 */
@Configuration
@ConditionalOnClass(JdbcTemplate.class)
public class SmsTemplateConfig {

    @Bean
    @ConditionalOnMissingBean(SmsTemplatePort.class)
    public SmsTemplatePort smsTemplatePort(JdbcTemplate jdbcTemplate) {
        return new JdbcSmsTemplateAdapter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(SmsTemplateService.class)
    public SmsTemplateService smsTemplateService(SmsTemplatePort smsTemplatePort) {
        return new SmsTemplateService(smsTemplatePort);
    }
}
