package com.vycepay.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.vycepay.auth.config.AuthProperties;
import com.vycepay.common.exception.BusinessException;

/**
 * In-memory fixed-window rate limiter for auth endpoints (login, OTP, forgot-pin).
 */
@Service
public class AuthRateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AuthProperties properties;
    private final Clock clock;

    @Autowired
    public AuthRateLimitService(AuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AuthRateLimitService(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Checks rate limit for a policy keyed by identifier (e.g. mobile or username).
     */
    public void check(String policy, String keyPart) {
        AuthProperties.Rule rule = properties.getRateLimit().rule(policy);
        if (rule == null || !rule.isEnabled()) {
            return;
        }
        String key = policy + ":" + (keyPart == null ? "unknown" : keyPart);
        long now = Instant.now(clock).toEpochMilli();
        Bucket bucket = buckets.compute(key, (ignored, current) -> current == null || current.windowEndsAt <= now
                ? new Bucket(1, now + rule.getWindowSeconds() * 1000L)
                : current.increment());
        if (bucket.count > rule.getLimit()) {
            throw new BusinessException("RATE_LIMITED", "Too many requests. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private record Bucket(int count, long windowEndsAt) {
        Bucket increment() {
            return new Bucket(count + 1, windowEndsAt);
        }
    }
}
