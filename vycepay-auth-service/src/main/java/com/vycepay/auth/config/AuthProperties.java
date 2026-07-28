package com.vycepay.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Auth-service security and rate-limit configuration.
 */
@ConfigurationProperties(prefix = "vycepay.auth")
public class AuthProperties {

    private final Pin pin = new Pin();
    private final RateLimit rateLimit = new RateLimit();

    public Pin getPin() {
        return pin;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public static class Pin {
        private int maxAttempts = 5;
        private int lockoutMinutes = 15;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getLockoutMinutes() {
            return lockoutMinutes;
        }

        public void setLockoutMinutes(int lockoutMinutes) {
            this.lockoutMinutes = lockoutMinutes;
        }
    }

    public static class RateLimit {
        private final Map<String, Rule> policies = new HashMap<>();

        public Map<String, Rule> getPolicies() {
            return policies;
        }

        public Rule rule(String policy) {
            return policies.get(policy);
        }
    }

    public static class Rule {
        private boolean enabled = true;
        private int limit = 10;
        private int windowSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
