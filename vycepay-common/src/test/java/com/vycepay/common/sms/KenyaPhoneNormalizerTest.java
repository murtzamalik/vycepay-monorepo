package com.vycepay.common.sms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KenyaPhoneNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "712345678,254712345678",
            "0712345678,254712345678",
            "254712345678,254712345678",
            "+254712345678,254712345678",
            "0115372786,254115372786",
            "115372786,254115372786"
    })
    void toRecipient_acceptsKeFormats(String raw, String expected) {
        assertEquals(expected, KenyaPhoneNormalizer.toRecipient(raw).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "abc", "+255712345678", "12345"})
    void toRecipient_rejectsInvalid(String raw) {
        assertTrue(KenyaPhoneNormalizer.toRecipient(raw).isEmpty());
    }

    @Test
    void redactOtpDigits() {
        assertEquals(
                "Your code is ******. Do not share.",
                KenyaPhoneNormalizer.redactOtpDigits("Your code is 123456. Do not share."));
    }

    @Test
    void maskRecipient() {
        assertEquals("254****5678", KenyaPhoneNormalizer.maskRecipient("254712345678"));
    }
}
