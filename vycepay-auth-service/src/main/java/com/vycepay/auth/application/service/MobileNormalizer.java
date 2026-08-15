package com.vycepay.auth.application.service;

import java.util.Optional;

/**
 * Normalizes raw contact dial strings to (mobileCountryCode, mobile) as stored on customer.
 * Kenya-first: default country code {@code 254}; national number is 9 digits without leading zero.
 */
public final class MobileNormalizer {

    public static final String DEFAULT_COUNTRY_CODE = "254";

    private MobileNormalizer() {
    }

    /**
     * Parses a raw mobile string into country code + national number.
     *
     * @param raw dial string from the address book (any common KE format)
     * @return normalized pair, or empty if unparseable
     */
    public static Optional<NormalizedMobile> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
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
            return Optional.empty();
        }
        String d = digits.toString();

        // +254XXXXXXXXX or 254XXXXXXXXX (12 digits: 254 + 9 national)
        if ((hadPlus || d.startsWith(DEFAULT_COUNTRY_CODE)) && d.startsWith(DEFAULT_COUNTRY_CODE) && d.length() == 12) {
            String national = d.substring(3);
            if (isKeNational(national)) {
                return Optional.of(new NormalizedMobile(DEFAULT_COUNTRY_CODE, national));
            }
            return Optional.empty();
        }

        // 0XXXXXXXXX (10 digits local with trunk 0)
        if (d.startsWith("0") && d.length() == 10) {
            String national = d.substring(1);
            if (isKeNational(national)) {
                return Optional.of(new NormalizedMobile(DEFAULT_COUNTRY_CODE, national));
            }
            return Optional.empty();
        }

        // XXXXXXXXX (9 digits national starting with 7)
        if (d.length() == 9 && isKeNational(d)) {
            return Optional.of(new NormalizedMobile(DEFAULT_COUNTRY_CODE, d));
        }

        return Optional.empty();
    }

    private static boolean isKeNational(String national) {
        return national != null && national.length() == 9 && national.charAt(0) == '7';
    }

    /**
     * Country code + national mobile as stored in {@code customer}.
     */
    public record NormalizedMobile(String mobileCountryCode, String mobile) {
        public String key() {
            return mobileCountryCode + ":" + mobile;
        }
    }
}
