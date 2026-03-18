package com.vycepay.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates JWT and extracts claims. Shared across services that need to verify tokens.
 */
public class JwtValidator {

    private static final String CLAIM_EXTERNAL_ID = "eid";

    private final SecretKey secretKey;

    public JwtValidator(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates token and returns external ID (subject).
     *
     * @param token Bearer token (without "Bearer " prefix)
     * @return External ID or null if invalid
     */
    public String validateAndGetExternalId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException e) {
            return null;
        }
    }
}
