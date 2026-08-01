package com.vycepay.common.config;

import com.vycepay.common.web.RequestIdFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Web configuration. Registers RequestIdFilter for log correlation.
 */
@Configuration
@ConditionalOnWebApplication
public class WebConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @ConditionalOnMissingBean(RequestIdFilter.class)
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }
}
