package com.vycepay.auth.application.facade;

import com.vycepay.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthFacadeMobileNormalizeTest {

    @Test
    void requireNormalizedMobile_stripsLeadingZero() {
        var n = AuthFacade.requireNormalizedMobile("254", "0796595339");
        assertEquals("254", n.mobileCountryCode());
        assertEquals("796595339", n.mobile());
    }

    @Test
    void requireNormalizedMobile_acceptsNineDigitNational() {
        var n = AuthFacade.requireNormalizedMobile("254", "712345678");
        assertEquals("254", n.mobileCountryCode());
        assertEquals("712345678", n.mobile());
    }

    @Test
    void requireNormalizedMobile_acceptsFullMsisdnInMobileField() {
        var n = AuthFacade.requireNormalizedMobile(null, "254712345678");
        assertEquals("254", n.mobileCountryCode());
        assertEquals("712345678", n.mobile());
    }

    @Test
    void requireNormalizedMobile_invalid_throwsInvalidMobile() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> AuthFacade.requireNormalizedMobile("254", "12345"));
        assertEquals("INVALID_MOBILE", ex.getCode());
    }
}
