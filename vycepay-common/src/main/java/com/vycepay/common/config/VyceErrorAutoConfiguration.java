package com.vycepay.common.config;

import com.vycepay.common.exception.GlobalExceptionHandler;
import com.vycepay.common.exception.VyceErrorCatalog;
import com.vycepay.common.security.JsonAccessDeniedHandler;
import com.vycepay.common.security.JsonAuthenticationEntryPoint;
import com.vycepay.common.web.RequestIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Registers shared error handling, request-id filter, and JSON security error handlers
 * for every service that depends on {@code vycepay-common}.
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class VyceErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public VyceErrorCatalog vyceErrorCatalog() {
        return new VyceErrorCatalog();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(VyceErrorCatalog catalog) {
        return new GlobalExceptionHandler(catalog);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @ConditionalOnMissingBean(RequestIdFilter.class)
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint(VyceErrorCatalog catalog) {
        return new JsonAuthenticationEntryPoint(catalog);
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonAccessDeniedHandler jsonAccessDeniedHandler(VyceErrorCatalog catalog) {
        return new JsonAccessDeniedHandler(catalog);
    }
}
