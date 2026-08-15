package com.vycepay.auth.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Kenya-first contact mobile normalization.
 */
class MobileNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "712345678,254,712345678",
            "0712345678,254,712345678",
            "254712345678,254,712345678",
            "+254712345678,254,712345678",
            "798765432,254,798765432",
            "0798765432,254,798765432",
            "254798765432,254,798765432",
            "+254798765432,254,798765432",
            "' 0712 345 678 ',254,712345678",
            "+254-712-345-678,254,712345678"
    })
    void normalize_keFormats(String input, String expectedCc, String expectedMobile) {
        var result = MobileNormalizer.normalize(input);
        assertTrue(result.isPresent());
        assertEquals(expectedCc, result.get().mobileCountryCode());
        assertEquals(expectedMobile, result.get().mobile());
        assertEquals(expectedCc + ":" + expectedMobile, result.get().key());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "abc", "123", "812345678", "0012345678", "+255712345678", "25471234567"})
    void normalize_invalid_returnsEmpty(String input) {
        assertTrue(MobileNormalizer.normalize(input).isEmpty());
    }

    @Test
    void normalize_rejectsNonKeNationalPrefix() {
        assertTrue(MobileNormalizer.normalize("612345678").isEmpty());
    }
}
