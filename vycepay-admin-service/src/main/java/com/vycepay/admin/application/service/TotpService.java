package com.vycepay.admin.application.service;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Verifies RFC 6238 compatible TOTP codes for enrolled admin users. */
@Service
public class TotpService {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int WINDOW = 1;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public TotpService() {
        this(Clock.systemUTC());
    }

    TotpService(Clock clock) {
        this.clock = clock;
    }

    public boolean verify(String base32Secret, String code) {
        if (base32Secret == null || base32Secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long counter = Instant.now(clock).getEpochSecond() / STEP_SECONDS;
        byte[] key = decodeBase32(base32Secret);
        for (int offset = -WINDOW; offset <= WINDOW; offset++) {
            if (totp(key, counter + offset).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String generateBase32Secret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    private String totp(byte[] key, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to verify TOTP", e);
        }
    }

    private byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteBuffer buffer = ByteBuffer.allocate(normalized.length() * 5 / 8);
        int bits = 0;
        int bitCount = 0;
        for (char c : normalized.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index < 0) {
                throw new BusinessException("ADMIN_MFA_SECRET_INVALID", "Stored MFA secret is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            bits = (bits << 5) | index;
            bitCount += 5;
            if (bitCount >= 8) {
                buffer.put((byte) ((bits >> (bitCount - 8)) & 0xFF));
                bitCount -= 8;
            }
        }
        buffer.flip();
        byte[] out = new byte[buffer.remaining()];
        buffer.get(out);
        return out;
    }

    private String encodeBase32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int bits = 0;
        int bitCount = 0;
        for (byte b : data) {
            bits = (bits << 8) | (b & 0xFF);
            bitCount += 8;
            while (bitCount >= 5) {
                out.append(ALPHABET.charAt((bits >> (bitCount - 5)) & 0x1F));
                bitCount -= 5;
            }
        }
        if (bitCount > 0) {
            out.append(ALPHABET.charAt((bits << (5 - bitCount)) & 0x1F));
        }
        return out.toString();
    }
}
