package com.vycepay.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.choicebank.ChoiceBankClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for Choice Bank API client.
 * Uses Resilience4j for retry and circuit breaker when available.
 */
@Configuration
public class ChoiceBankClientConfig {

    private static final String RESILIENCE_INSTANCE = "choiceBank";

    @Bean
    public ChoiceBankClient choiceBankClient(
            @Value("${vycepay.choice-bank.base-url:https://baas-pilot.choicebankapi.com}") String baseUrl,
            @Value("${vycepay.choice-bank.sender-id}") String senderId,
            @Value("${vycepay.choice-bank.private-key}") String privateKey,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Autowired(required = false) CircuitBreakerRegistry circuitBreakerRegistry,
            @Autowired(required = false) RetryRegistry retryRegistry) {
        CircuitBreaker cb = circuitBreakerRegistry != null
                ? circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE) : null;
        Retry retry = retryRegistry != null ? retryRegistry.retry(RESILIENCE_INSTANCE) : null;
        return new ChoiceBankClient(baseUrl, senderId, privateKey, restTemplate, objectMapper, cb, retry);
    }
}
