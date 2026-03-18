package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response containing JWT token.
 */
@Schema(description = "Authentication response with JWT")
public class AuthResponse {

    @Schema(description = "JWT bearer token for API authentication")
    private String token;

    @Schema(description = "External customer ID (use as X-Customer-Id for downstream APIs)")
    private String externalId;

    @Schema(description = "Token validity duration in seconds")
    private long expiresIn;

    public AuthResponse() {
    }

    public AuthResponse(String token, String externalId, long expiresIn) {
        this.token = token;
        this.externalId = externalId;
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
