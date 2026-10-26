package com.vycepay.common.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Object storage settings for KYC biometric media ({@code vycepay.media.*}).
 */
@ConfigurationProperties(prefix = "vycepay.media")
public class MediaProperties {

    /** local | s3 */
    private String type = "local";
    private String localBasePath = "./data/kyc-media";
    private String bucket;
    private String region = "af-south-1";
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess = false;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocalBasePath() {
        return localBasePath;
    }

    public void setLocalBasePath(String localBasePath) {
        this.localBasePath = localBasePath;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }
}
