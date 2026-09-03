package com.vycepay.common.sms.port;

/**
 * Outbound port for sending SMS via an external provider (e.g. MobiWave).
 */
public interface SmsPort {

    /**
     * Sends a single SMS. Implementations must not throw for provider failures;
     * return a failed/skipped {@link SmsSendResult} instead.
     *
     * @param request recipient and message body
     * @return delivery outcome (never null)
     */
    SmsSendResult send(SmsSendRequest request);

    /**
     * Fetches provider SMS unit balance when supported.
     *
     * @return balance payload or error message; never null
     */
    SmsBalanceResult balance();
}
