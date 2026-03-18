package com.vycepay.common.config;

import com.vycepay.common.web.RequestIdFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Web configuration. Registers RequestIdFilter for log correlation.
 */
@Configuration
@ConditionalOnWebApplication
public class WebConfig {

    @Bean
    public OncePerRequestFilter requestIdFilter() {
        return new RequestIdFilter();
    }
}
