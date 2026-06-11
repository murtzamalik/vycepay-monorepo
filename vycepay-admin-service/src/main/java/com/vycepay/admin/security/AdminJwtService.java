package com.vycepay.admin.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.vycepay.admin.config.AdminProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Issues and validates admin-only JWTs. The jti is checked against admin_session on every request. */
@Service
public class AdminJwtService {
    private static final Logger log = LoggerFactory.getLogger(AdminJwtService.class);
    private final SecretKey secretKey;
    private final long expirationMs;
    public AdminJwtService(AdminProperties properties, Environment environment) {
        String secret = properties.getJwt().getSecret();
        validateSecret(secret, environment);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.getJwt().getExpirationMs();
    }
    public String createToken(String externalId, String username, String jti) {
        Instant now = Instant.now();
        return Jwts.builder().issuer("vycepay-admin").subject(externalId).claim("username", username).id(jti)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expirationMs))).signWith(secretKey).compact();
    }
    public Claims parse(String token) {
        try {
            Jws<Claims> claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return claims.getPayload();
        } catch (JwtException e) {
            log.debug("Invalid admin JWT: {}", e.getMessage());
            return null;
        }
    }
    public long getExpirationMs() { return expirationMs; }

    private void validateSecret(String secret, Environment environment) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ADMIN_JWT_SECRET must be set to at least 32 bytes");
        }
        boolean prod = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod && secret.toLowerCase(java.util.Locale.ROOT).contains("dev-only")) {
            throw new IllegalStateException("ADMIN_JWT_SECRET must not use a development-only value in prod");
        }
    }
}
