package com.vycepay.admin.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vycepay.admin.config.AdminProperties;
import com.vycepay.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** In-memory fixed-window rate limiter for sensitive admin endpoints. */
@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AdminProperties properties;
    private final Clock clock;

    @Autowired
    public RateLimitService(AdminProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RateLimitService(AdminProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void check(String policy, HttpServletRequest request) {
        AdminProperties.RateLimit.Rule rule = properties.getRateLimit().rule(policy);
        if (rule == null || !rule.isEnabled()) {
            return;
        }
        String key = policy + ":" + AdminAuditService.clientIp(request);
        long now = Instant.now(clock).toEpochMilli();
        Bucket bucket = buckets.compute(key, (ignored, current) -> current == null || current.windowEndsAt <= now
                ? new Bucket(1, now + rule.getWindowSeconds() * 1000L)
                : current.increment());
        if (bucket.count > rule.getLimit()) {
            throw new BusinessException("ADMIN_RATE_LIMITED", "Too many admin requests. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private record Bucket(int count, long windowEndsAt) {
        Bucket increment() {
            return new Bucket(count + 1, windowEndsAt);
        }
    }
}
