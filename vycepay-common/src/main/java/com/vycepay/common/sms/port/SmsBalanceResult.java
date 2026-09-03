package com.vycepay.common.sms.port;

import java.util.Map;

/**
 * Provider SMS balance lookup result.
 *
 * @param success true when balance retrieved
 * @param data    provider payload (units/balance fields vary by provider)
 * @param errorMessage failure reason when success is false
 */
public record SmsBalanceResult(boolean success, Map<String, Object> data, String errorMessage) {

    public static SmsBalanceResult ok(Map<String, Object> data) {
        return new SmsBalanceResult(true, data != null ? data : Map.of(), null);
    }

    public static SmsBalanceResult failed(String errorMessage) {
        return new SmsBalanceResult(false, Map.of(), errorMessage);
    }
}
