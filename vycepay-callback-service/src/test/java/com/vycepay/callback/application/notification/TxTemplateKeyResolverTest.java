package com.vycepay.callback.application.notification;

import com.vycepay.common.sms.template.SmsTemplateKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TxTemplateKeyResolverTest {

    @ParameterizedTest
    @CsvSource({
            "PAY_TILL, true, true, TX_PAY_TILL_SUCCESS",
            "PAY_BILL, true, true, TX_PAY_BILL_SUCCESS",
            "MOBILE_MONEY, true, true, TX_MOBILE_MONEY_SUCCESS",
            "INTERNAL_TRANSFER, true, true, TX_TRANSFER_SUCCESS",
            "PAY_TILL, false, true, TX_INBOUND_SUCCESS",
            "UNKNOWN, true, true, TX_DEFAULT_SUCCESS",
            ", true, true, TX_DEFAULT_SUCCESS",
            "PAY_TILL, true, false, TX_FAILED",
            "PAY_BILL, false, false, TX_FAILED",
    })
    void resolve_mapsChannelDirectionOutcome(String channel, boolean outbound, boolean success, String expected) {
        assertEquals(expected, TxTemplateKeyResolver.resolve(
                channel == null || channel.isBlank() ? null : channel, outbound, success));
    }

    @Test
    void resolve_blankChannelOutboundSuccess_isDefault() {
        assertEquals(SmsTemplateKeys.TX_DEFAULT_SUCCESS,
                TxTemplateKeyResolver.resolve("  ", true, true));
    }
}
