package com.vycepay.common.kyc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.vycepay.common.exception.BusinessException;

class Base64MediaTest {

    @Test
    void stripsDataUri() {
        String raw = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});
        assertEquals(raw, Base64Media.stripDataUri("data:image/jpeg;base64," + raw));
    }

    @Test
    void decodeRequiredRejectsBlank() {
        assertThrows(BusinessException.class, () -> Base64Media.decodeRequired("  ", "selfie"));
    }
}
