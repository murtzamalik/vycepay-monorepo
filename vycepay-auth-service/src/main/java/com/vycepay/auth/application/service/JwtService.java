package com.vycepay.auth.application.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and validates JWT tokens for authenticated sessions.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_CUSTOMER_ID = "cid";
    private static final String CLAIM_EXTERNAL_ID = "eid";

    private final SecretKey secretKey;
    private final long validityMs;

    public JwtService(@Value("${vycepay.jwt.secret:vycepay-default-secret-key-min-256-bits-for-hs256}") String secret,
                      @Value("${vycepay.jwt.expiry-minutes:10}") long expiryMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMs = expiryMinutes * 60 * 1000;
    }

    public long getValiditySeconds() {
        return validityMs / 1000;
    }

    /**
     * Generates JWT for customer.
     *
     * @param customerId Internal customer ID
     * @param externalId Public external ID for APIs
     * @return Signed JWT string
     */
    public String createToken(Long customerId, String externalId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);
        return Jwts.builder()
                .subject(externalId)
                .claim(CLAIM_CUSTOMER_ID, customerId)
                .claim(CLAIM_EXTERNAL_ID, externalId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses and validates token. Returns external ID (subject) if valid.
     *
     * @param token JWT string
     * @return External ID or null if invalid
     */
    public String parseToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload().getSubject();
        } catch (JwtException e) {
            log.debug("Invalid token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts customer ID from token.
     */
    public Long getCustomerIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object cid = claims.get(CLAIM_CUSTOMER_ID);
            return cid instanceof Number ? ((Number) cid).longValue() : null;
        } catch (JwtException e) {
            return null;
        }
    }
}
