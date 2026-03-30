package com.vycepay.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * In-memory ring buffer of recent Choice BaaS HTTP calls for debugging (per JVM).
 */
@ConfigurationProperties(prefix = "vycepay.choice-bank.audit.http")
public class ChoiceBankHttpAuditProperties {

    /**
     * When true, outbound calls are recorded and exposed via GET /internal/choice-bank/http-traces.
     */
    private boolean enabled = true;

    /**
     * Max entries retained (oldest dropped).
     */
    private int maxEntries = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }
}
