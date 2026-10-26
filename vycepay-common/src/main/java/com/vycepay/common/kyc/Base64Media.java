package com.vycepay.common.kyc;

import java.util.Base64;

import org.springframework.http.HttpStatus;

import com.vycepay.common.exception.BusinessException;

/**
 * Helpers for Base64 image/video payloads (data-URI strip + decode).
 */
public final class Base64Media {

    private Base64Media() {
    }

    public static String stripDataUri(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        int idx = trimmed.indexOf("base64,");
        if (trimmed.startsWith("data:") && idx >= 0) {
            return trimmed.substring(idx + "base64,".length());
        }
        return trimmed;
    }

    public static byte[] decodeRequired(String input, String fieldName) {
        String b64 = stripDataUri(input);
        if (b64 == null || b64.isBlank()) {
            throw new BusinessException("KYC_BIOMETRIC_BAD_INPUT",
                    fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        try {
            return Base64.getDecoder().decode(b64.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("KYC_BIOMETRIC_BAD_INPUT",
                    fieldName + " is not valid Base64", HttpStatus.BAD_REQUEST);
        }
    }

    public static String normalize(String input) {
        return stripDataUri(input);
    }
}
