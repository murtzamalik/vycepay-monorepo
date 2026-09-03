package com.vycepay.common.sms;

import com.vycepay.common.sms.port.SmsBalanceResult;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op SMS adapter for local/dev when {@code vycepay.sms.enabled=false}.
 * Logs masked recipient only; never logs message body (may contain OTP).
 */
public class LoggingSmsAdapter implements SmsPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsAdapter.class);

    @Override
    public SmsSendResult send(SmsSendRequest request) {
        String masked = request != null
                ? KenyaPhoneNormalizer.maskRecipient(request.recipient())
                : "****";
        log.info("SMS skipped (disabled): recipient={}", masked);
        return SmsSendResult.skipped("SMS_DISABLED");
    }

    @Override
    public SmsBalanceResult balance() {
        return SmsBalanceResult.failed("SMS is disabled");
    }
}
