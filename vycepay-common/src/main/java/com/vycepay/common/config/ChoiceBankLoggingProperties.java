package com.vycepay.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls outbound Choice BaaS HTTP request/response logging in {@link com.vycepay.common.choicebank.ChoiceBankClient}.
 */
@ConfigurationProperties(prefix = "vycepay.choice-bank.logging")
public class ChoiceBankLoggingProperties {

    /**
     * Master switch for choice_baas_request / choice_baas_response logs.
     */
    private boolean enabled = true;

    /**
     * When true, log JSON payloads; when false, log only path, requestId, and response code/msg.
     */
    private boolean logBodies = true;

    /**
     * When true, replace signature and salt fields in logged JSON with [REDACTED].
     */
    private boolean redactSignatures = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogBodies() {
        return logBodies;
    }

    public void setLogBodies(boolean logBodies) {
        this.logBodies = logBodies;
    }

    public boolean isRedactSignatures() {
        return redactSignatures;
    }

    public void setRedactSignatures(boolean redactSignatures) {
        this.redactSignatures = redactSignatures;
    }
}
