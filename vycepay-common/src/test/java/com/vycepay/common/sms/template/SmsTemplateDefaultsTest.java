package com.vycepay.common.sms.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Catalog integrity: defaults must cover every seeded production key.
 */
class SmsTemplateDefaultsTest {

    @Test
    void allCatalogKeysHaveDefaultBodies() {
        assertEquals(11, SmsTemplateDefaults.allKeys().size());
        assertTrue(SmsTemplateDefaults.isKnownKey(SmsTemplateKeys.OTP_SIGNUP));
        assertTrue(SmsTemplateDefaults.isKnownKey(SmsTemplateKeys.TX_PAY_TILL_SUCCESS));
        assertTrue(SmsTemplateDefaults.isKnownKey(SmsTemplateKeys.TX_FAILED));
        assertTrue(SmsTemplateDefaults.isKnownKey(SmsTemplateKeys.TX_DEFAULT_SUCCESS));
        assertFalse(SmsTemplateDefaults.isKnownKey("NOT_A_REAL_KEY"));
        assertFalse(SmsTemplateDefaults.isKnownKey(null));
        for (String key : SmsTemplateDefaults.allKeys()) {
            String body = SmsTemplateDefaults.bodyFor(key);
            assertFalse(body.isBlank(), "blank default for " + key);
            assertTrue(body.length() <= 640, key);
        }
    }
}
