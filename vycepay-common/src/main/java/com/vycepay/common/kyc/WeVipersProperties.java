package com.vycepay.common.kyc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WeVipers / BBS-SDKs KYC API client settings ({@code vycepay.kyc.wevipers.*}).
 */
@ConfigurationProperties(prefix = "vycepay.kyc.wevipers")
public class WeVipersProperties {

    private boolean enabled = true;
    private String baseUrl = "http://2.28.204.253:8080";
    private int connectTimeoutMs = 5000;
    private int ocrReadTimeoutMs = 60000;
    private int livenessReadTimeoutMs = 120000;
    private int matchReadTimeoutMs = 30000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getOcrReadTimeoutMs() { return ocrReadTimeoutMs; }
    public void setOcrReadTimeoutMs(int ocrReadTimeoutMs) { this.ocrReadTimeoutMs = ocrReadTimeoutMs; }
    public int getLivenessReadTimeoutMs() { return livenessReadTimeoutMs; }
    public void setLivenessReadTimeoutMs(int livenessReadTimeoutMs) { this.livenessReadTimeoutMs = livenessReadTimeoutMs; }
    public int getMatchReadTimeoutMs() { return matchReadTimeoutMs; }
    public void setMatchReadTimeoutMs(int matchReadTimeoutMs) { this.matchReadTimeoutMs = matchReadTimeoutMs; }
}
