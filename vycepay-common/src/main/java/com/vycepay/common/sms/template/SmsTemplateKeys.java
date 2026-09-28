package com.vycepay.common.sms.template;

/**
 * Catalog of system SMS template keys (one active body per key in {@code sms_template}).
 */
public final class SmsTemplateKeys {

    public static final String OTP_SIGNUP = "OTP_SIGNUP";
    public static final String OTP_DEVICE_BIND = "OTP_DEVICE_BIND";
    public static final String OTP_PIN_RESET = "OTP_PIN_RESET";
    public static final String OTP_CREDENTIALS_MIGRATE = "OTP_CREDENTIALS_MIGRATE";

    public static final String TX_PAY_TILL_SUCCESS = "TX_PAY_TILL_SUCCESS";
    public static final String TX_PAY_BILL_SUCCESS = "TX_PAY_BILL_SUCCESS";
    public static final String TX_MOBILE_MONEY_SUCCESS = "TX_MOBILE_MONEY_SUCCESS";
    public static final String TX_TRANSFER_SUCCESS = "TX_TRANSFER_SUCCESS";
    public static final String TX_INBOUND_SUCCESS = "TX_INBOUND_SUCCESS";
    public static final String TX_FAILED = "TX_FAILED";
    public static final String TX_DEFAULT_SUCCESS = "TX_DEFAULT_SUCCESS";

    public static final String CATEGORY_OTP = "OTP";
    public static final String CATEGORY_TRANSACTION = "TRANSACTION";

    private SmsTemplateKeys() {
    }
}
