package com.vycepay.common.sms;

import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggingSmsAdapterTest {

    @Test
    void send_returnsSkipped() {
        LoggingSmsAdapter adapter = new LoggingSmsAdapter();
        SmsSendResult result = adapter.send(new SmsSendRequest("254712345678", "secret otp 999999"));
        assertEquals(SmsSendResult.SKIPPED, result.status());
        assertEquals("SMS_DISABLED", result.errorMessage());
    }
}
