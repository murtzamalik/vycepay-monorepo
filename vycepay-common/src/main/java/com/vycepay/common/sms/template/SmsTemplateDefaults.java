package com.vycepay.common.sms.template;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compile-time fallback bodies matching Flyway V15 seed (SMS never blank for known keys).
 */
public final class SmsTemplateDefaults {

    private static final Map<String, String> BODIES = new LinkedHashMap<>();

    static {
        BODIES.put(SmsTemplateKeys.OTP_SIGNUP,
                "Your VycePay verification code is {otp}. Valid for {valid_minutes} minutes. Do not share.");
        BODIES.put(SmsTemplateKeys.OTP_DEVICE_BIND,
                "Your VycePay new device login code is {otp}. Valid for {valid_minutes} minutes. Do not share.");
        BODIES.put(SmsTemplateKeys.OTP_PIN_RESET,
                "Your VycePay PIN reset code is {otp}. Valid for {valid_minutes} minutes. Do not share.");
        BODIES.put(SmsTemplateKeys.OTP_CREDENTIALS_MIGRATE,
                "Your VycePay account setup code is {otp}. Valid for {valid_minutes} minutes. Do not share.");
        BODIES.put(SmsTemplateKeys.TX_PAY_TILL_SUCCESS,
                "Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}");
        BODIES.put(SmsTemplateKeys.TX_PAY_BILL_SUCCESS,
                "Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}");
        BODIES.put(SmsTemplateKeys.TX_MOBILE_MONEY_SUCCESS,
                "Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}");
        BODIES.put(SmsTemplateKeys.TX_TRANSFER_SUCCESS,
                "Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}");
        BODIES.put(SmsTemplateKeys.TX_INBOUND_SUCCESS,
                "Received {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}");
        BODIES.put(SmsTemplateKeys.TX_FAILED,
                "Your transaction of {currency} {amount} failed.{error_suffix} Ref: {ref}");
        BODIES.put(SmsTemplateKeys.TX_DEFAULT_SUCCESS,
                "Sent {currency} {amount}{counterparty_suffix}. From {from_account}. To {to_account}. Ref: {ref}. {date}, {time}");
    }

    private SmsTemplateDefaults() {
    }

    public static String bodyFor(String templateKey) {
        if (templateKey == null) {
            return "VycePay notification.";
        }
        return BODIES.getOrDefault(templateKey, "VycePay notification.");
    }

    public static boolean isKnownKey(String templateKey) {
        return templateKey != null && BODIES.containsKey(templateKey);
    }

    public static List<String> allKeys() {
        return List.copyOf(BODIES.keySet());
    }

    public static Map<String, String> allBodies() {
        return Map.copyOf(BODIES);
    }
}
