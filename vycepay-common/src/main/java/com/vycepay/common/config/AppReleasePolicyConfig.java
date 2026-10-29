package com.vycepay.common.config;

import com.vycepay.common.apprelease.AppReleasePolicyPort;
import com.vycepay.common.apprelease.AppReleasePolicyService;
import com.vycepay.common.apprelease.JdbcAppReleasePolicyAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wires app release policy load for services that have JDBC on the classpath.
 * Same {@code @Import} timing notes as {@link SmsTemplateConfig}.
 */
@Configuration
@ConditionalOnClass(JdbcTemplate.class)
public class AppReleasePolicyConfig {

    @Bean
    @ConditionalOnMissingBean(AppReleasePolicyPort.class)
    public AppReleasePolicyPort appReleasePolicyPort(JdbcTemplate jdbcTemplate) {
        return new JdbcAppReleasePolicyAdapter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(AppReleasePolicyService.class)
    public AppReleasePolicyService appReleasePolicyService(AppReleasePolicyPort appReleasePolicyPort) {
        return new AppReleasePolicyService(appReleasePolicyPort);
    }
}
