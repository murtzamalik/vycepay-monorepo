package com.vycepay.kyc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Biometric KYC session settings ({@code vycepay.kyc.biometric.*}).
 */
@ConfigurationProperties(prefix = "vycepay.kyc.biometric")
public class BiometricProperties {

    /** When true, Choice submit requires READY_TO_SUBMIT session. */
    private boolean required = false;
    private int sessionTtlHours = 24;
    private int maxAttempts = 5;

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public int getSessionTtlHours() { return sessionTtlHours; }
    public void setSessionTtlHours(int sessionTtlHours) { this.sessionTtlHours = sessionTtlHours; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
}
