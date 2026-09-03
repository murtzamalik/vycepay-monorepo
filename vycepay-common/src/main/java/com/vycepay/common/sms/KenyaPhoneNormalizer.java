package com.vycepay.common.sms;

/**
 * Kenya-first phone normalization for SMS recipients (MobiWave: digits without plus).
 */
public final class KenyaPhoneNormalizer {

    public static final String DEFAULT_COUNTRY_CODE = "254";

    private KenyaPhoneNormalizer() {
    }

    /**
     * Normalizes a raw dial string to {@code 254} + 9-digit national number.
     *
     * @param raw phone string in common KE formats
     * @return normalized recipient, or empty if invalid
     */
    public static java.util.Optional<String> toRecipient(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.Optional.empty();
        }
        String trimmed = raw.trim();
        boolean hadPlus = trimmed.startsWith("+");
        StringBuilder digits = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.isEmpty()) {
            return java.util.Optional.empty();
        }
        String d = digits.toString();

        if ((hadPlus || d.startsWith(DEFAULT_COUNTRY_CODE)) && d.startsWith(DEFAULT_COUNTRY_CODE) && d.length() == 12) {
            String national = d.substring(3);
            if (isKeNational(national)) {
                return java.util.Optional.of(DEFAULT_COUNTRY_CODE + national);
            }
            return java.util.Optional.empty();
        }
        if (d.startsWith("0") && d.length() == 10) {
            String national = d.substring(1);
            if (isKeNational(national)) {
                return java.util.Optional.of(DEFAULT_COUNTRY_CODE + national);
            }
            return java.util.Optional.empty();
        }
        if (d.length() == 9 && isKeNational(d)) {
            return java.util.Optional.of(DEFAULT_COUNTRY_CODE + d);
        }
        return java.util.Optional.empty();
    }

    /**
     * Masks middle digits for display/logs: {@code 254****5678}.
     */
    public static String maskRecipient(String recipient) {
        if (recipient == null || recipient.length() < 8) {
            return "****";
        }
        return recipient.substring(0, 3) + "****" + recipient.substring(recipient.length() - 4);
    }

    /**
     * Redacts contiguous digit runs of length >= 4 (OTP codes) in a message body.
     */
    public static String redactOtpDigits(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("\\d{4,}", "******");
    }

    private static boolean isKeNational(String national) {
        return national != null && national.length() == 9 && national.charAt(0) == '7';
    }
}
