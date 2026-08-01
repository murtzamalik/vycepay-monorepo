package com.vycepay.bff.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vycepay.bff")
public class BffBackendProperties {

    private String authUrl = "http://127.0.0.1:9091";
    private String kycUrl = "http://127.0.0.1:9092";
    private String walletsUrl = "http://127.0.0.1:9093";
    private String transactionsUrl = "http://127.0.0.1:9094";
    private String activityUrl = "http://127.0.0.1:9095";
    private String callbackUrl = "http://127.0.0.1:8081";

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
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    /**
     * First path segment under /api/v1/ -> backend base URL (no trailing slash).
     */
    public Map<String, String> pathPrefixToBaseUrl() {
        Map<String, String> m = new HashMap<>();
        m.put("auth", trim(authUrl));
        m.put("kyc", trim(kycUrl));
        m.put("wallets", trim(walletsUrl));
        m.put("transactions", trim(transactionsUrl));
        m.put("activity", trim(activityUrl));
        m.put("notifications", trim(callbackUrl));
        return m;
    }

    private static String trim(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
