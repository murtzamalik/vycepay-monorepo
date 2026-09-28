package com.vycepay.callback.application.notification;

import com.vycepay.common.sms.template.SmsTemplateKeys;

/**
 * Maps money-event channel / direction / outcome to an SMS template key.
 */
public final class TxTemplateKeyResolver {

    private TxTemplateKeyResolver() {
    }

    public static String resolve(String paymentChannel, boolean outbound, boolean success) {
        if (!success) {
            return SmsTemplateKeys.TX_FAILED;
        }
        if (!outbound) {
            return SmsTemplateKeys.TX_INBOUND_SUCCESS;
        }
        if (paymentChannel == null || paymentChannel.isBlank()) {
            return SmsTemplateKeys.TX_DEFAULT_SUCCESS;
        }
        return switch (paymentChannel.trim().toUpperCase()) {
            case "PAY_TILL" -> SmsTemplateKeys.TX_PAY_TILL_SUCCESS;
            case "PAY_BILL" -> SmsTemplateKeys.TX_PAY_BILL_SUCCESS;
            case "MOBILE_MONEY" -> SmsTemplateKeys.TX_MOBILE_MONEY_SUCCESS;
            case "INTERNAL_TRANSFER" -> SmsTemplateKeys.TX_TRANSFER_SUCCESS;
            default -> SmsTemplateKeys.TX_DEFAULT_SUCCESS;
        };
    }
}
