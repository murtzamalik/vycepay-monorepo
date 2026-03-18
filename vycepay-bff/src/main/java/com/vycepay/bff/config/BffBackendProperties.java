package com.vycepay.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "vycepay.bff")
public class BffBackendProperties {

    private String authUrl = "http://127.0.0.1:8082";
    private String kycUrl = "http://127.0.0.1:8083";
    private String walletsUrl = "http://127.0.0.1:8084";
    private String transactionsUrl = "http://127.0.0.1:8085";
    private String activityUrl = "http://127.0.0.1:8086";

    public String getAuthUrl() { return authUrl; }
    public void setAuthUrl(String authUrl) { this.authUrl = authUrl; }
    public String getKycUrl() { return kycUrl; }
    public void setKycUrl(String kycUrl) { this.kycUrl = kycUrl; }
    public String getWalletsUrl() { return walletsUrl; }
    public void setWalletsUrl(String walletsUrl) { this.walletsUrl = walletsUrl; }
    public String getTransactionsUrl() { return transactionsUrl; }
    public void setTransactionsUrl(String transactionsUrl) { this.transactionsUrl = transactionsUrl; }
    public String getActivityUrl() { return activityUrl; }
    public void setActivityUrl(String activityUrl) { this.activityUrl = activityUrl; }

    /**
     * First path segment under /api/v1/ -> backend base URL (no trailing slash).
     */
    public Map<String, String> pathPrefixToBaseUrl() {
        Map<String, String> m = new HashMap<>();
        m.put("auth", authUrl.endsWith("/") ? authUrl.substring(0, authUrl.length() - 1) : authUrl);
        m.put("kyc", kycUrl.endsWith("/") ? kycUrl.substring(0, kycUrl.length() - 1) : kycUrl);
        m.put("wallets", walletsUrl.endsWith("/") ? walletsUrl.substring(0, walletsUrl.length() - 1) : walletsUrl);
        m.put("transactions", transactionsUrl.endsWith("/") ? transactionsUrl.substring(0, transactionsUrl.length() - 1) : transactionsUrl);
        m.put("activity", activityUrl.endsWith("/") ? activityUrl.substring(0, activityUrl.length() - 1) : activityUrl);
        return m;
    }
}
