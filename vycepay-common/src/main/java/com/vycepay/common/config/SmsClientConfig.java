package com.vycepay.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.sms.LoggingSmsAdapter;
import com.vycepay.common.sms.MobiWaveSmsAdapter;
import com.vycepay.common.sms.port.SmsPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Wires {@link SmsPort}: MobiWave when enabled, otherwise logging no-op.
 * Import explicitly from auth/admin apps (narrow component scan).
 */
@Configuration
public class SmsClientConfig {

    private static final String RESILIENCE_INSTANCE = "mobiWave";

    @Bean
    @ConditionalOnMissingBean(SmsPort.class)
    public SmsPort smsPort(
            @Value("${vycepay.sms.enabled:false}") boolean enabled,
            @Value("${vycepay.sms.base-url:https://sms.mobiwave.co.ke/api/v3}") String baseUrl,
            @Value("${vycepay.sms.api-token:}") String apiToken,
            @Value("${vycepay.sms.sender-id:VycePay}") String senderId,
            @Autowired(required = false) RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Autowired(required = false) CircuitBreakerRegistry circuitBreakerRegistry,
            @Autowired(required = false) RetryRegistry retryRegistry) {
        if (!enabled) {
            return new LoggingSmsAdapter();
        }
        RestTemplate rt = restTemplate != null ? restTemplate : new RestTemplate();
        CircuitBreaker cb = circuitBreakerRegistry != null
                ? circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE) : null;
        Retry retry = retryRegistry != null ? retryRegistry.retry(RESILIENCE_INSTANCE) : null;
        return new MobiWaveSmsAdapter(baseUrl, apiToken, senderId, rt, objectMapper, cb, retry);
    }
}
