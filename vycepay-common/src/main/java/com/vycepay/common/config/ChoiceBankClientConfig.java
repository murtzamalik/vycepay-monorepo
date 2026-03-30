package com.vycepay.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.choicebank.ChoiceBankClient;
import com.vycepay.common.choicebank.ChoiceBankHttpAuditStore;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for Choice Bank API client.
 * Uses Resilience4j for retry and circuit breaker when available.
 */
@Configuration
@EnableConfigurationProperties({ChoiceBankLoggingProperties.class, ChoiceBankHttpAuditProperties.class})
public class ChoiceBankClientConfig {

    private static final String RESILIENCE_INSTANCE = "choiceBank";

    /**
     * In-memory ring buffer of recent Choice HTTP calls; disabled when {@code vycepay.choice-bank.audit.http.enabled=false}.
     */
    @Bean
    @ConditionalOnProperty(prefix = "vycepay.choice-bank.audit.http", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ChoiceBankHttpAuditStore choiceBankHttpAuditStore(ChoiceBankHttpAuditProperties auditProperties) {
        return new ChoiceBankHttpAuditStore(auditProperties.getMaxEntries());
    }

    @Bean
    public ChoiceBankClient choiceBankClient(
            @Value("${vycepay.choice-bank.base-url:https://baas-pilot.choicebankapi.com}") String baseUrl,
            @Value("${vycepay.choice-bank.sender-id}") String senderId,
            @Value("${vycepay.choice-bank.private-key}") String privateKey,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            ChoiceBankLoggingProperties loggingProperties,
            @Autowired(required = false) ChoiceBankHttpAuditStore auditStore,
            @Autowired(required = false) CircuitBreakerRegistry circuitBreakerRegistry,
            @Autowired(required = false) RetryRegistry retryRegistry) {
        CircuitBreaker cb = circuitBreakerRegistry != null
                ? circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE) : null;
        Retry retry = retryRegistry != null ? retryRegistry.retry(RESILIENCE_INSTANCE) : null;
        return new ChoiceBankClient(baseUrl, senderId, privateKey, restTemplate, objectMapper, cb, retry,
                loggingProperties.isEnabled(),
                loggingProperties.isLogBodies(),
                loggingProperties.isRedactSignatures(),
                auditStore);
    }
}
