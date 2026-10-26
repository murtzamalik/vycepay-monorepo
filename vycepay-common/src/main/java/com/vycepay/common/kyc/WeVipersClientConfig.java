package com.vycepay.common.kyc;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.kyc.port.DocumentVerificationPort;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

/**
 * Wires WeVipers {@link DocumentVerificationPort} with per-operation timeouts and resilience.
 */
@Configuration
@EnableConfigurationProperties(WeVipersProperties.class)
@ConditionalOnProperty(prefix = "vycepay.kyc.wevipers", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WeVipersClientConfig {

    private static final String RESILIENCE_INSTANCE = "weVipersKyc";

    @Bean
    public DocumentVerificationPort documentVerificationPort(
            WeVipersProperties props,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Autowired(required = false) CircuitBreakerRegistry circuitBreakerRegistry,
            @Autowired(required = false) RetryRegistry retryRegistry) {
        RestTemplate ocr = build(restTemplateBuilder, props.getConnectTimeoutMs(), props.getOcrReadTimeoutMs());
        RestTemplate liveness = build(restTemplateBuilder, props.getConnectTimeoutMs(), props.getLivenessReadTimeoutMs());
        RestTemplate match = build(restTemplateBuilder, props.getConnectTimeoutMs(), props.getMatchReadTimeoutMs());
        CircuitBreaker cb = circuitBreakerRegistry != null
                ? circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE) : null;
        Retry retry = retryRegistry != null ? retryRegistry.retry(RESILIENCE_INSTANCE) : null;
        return new WeVipersKycAdapter(props.getBaseUrl(), ocr, liveness, match, objectMapper, cb, retry);
    }

    private static RestTemplate build(RestTemplateBuilder builder, int connectMs, int readMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectMs))
                .setReadTimeout(Duration.ofMillis(readMs))
                .build();
    }
}
