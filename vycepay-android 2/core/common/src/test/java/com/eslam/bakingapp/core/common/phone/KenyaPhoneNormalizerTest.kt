package com.eslam.bakingapp.core.common.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KenyaPhoneNormalizerTest {

    @Test
    fun toNationalMobile_stripsLeadingZero() {
        assertEquals("796595339", KenyaPhoneNormalizer.toNationalMobile("0796595339"))
    }

    @Test
    fun toNationalMobile_strips254() {
        assertEquals("712345678", KenyaPhoneNormalizer.toNationalMobile("254712345678"))
    }

    @Test
    fun toNationalMobile_nineDigit() {
        assertEquals("712345678", KenyaPhoneNormalizer.toNationalMobile("712345678"))
    }

    @Test
    fun toNationalMobile_invalid() {
        assertNull(KenyaPhoneNormalizer.toNationalMobile("12345"))
        assertNull(KenyaPhoneNormalizer.toNationalMobile("0812345678"))
    }

    @Test
    fun toMsisdn254() {
        assertEquals("254796595339", KenyaPhoneNormalizer.toMsisdn254("0796595339"))
    }
}
