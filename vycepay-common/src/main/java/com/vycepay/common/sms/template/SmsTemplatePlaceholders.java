package com.vycepay.common.sms.template;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Allowed placeholder names and sample values for admin edit/preview UIs.
 */
public final class SmsTemplatePlaceholders {

    private static final List<String> OTP = List.of("otp", "purpose_label", "valid_minutes");

    private static final List<String> TRANSACTION = List.of(
            "amount", "currency", "counterparty", "counterparty_suffix",
            "to_account", "from_account", "ref", "vyce_ref",
            "channel", "channel_label", "date", "time", "status_label",
            "error_msg", "error_suffix");

    private SmsTemplatePlaceholders() {
    }

    public static List<String> forCategory(String category) {
        if (SmsTemplateKeys.CATEGORY_OTP.equalsIgnoreCase(category)) {
            return OTP;
        }
        if (SmsTemplateKeys.CATEGORY_TRANSACTION.equalsIgnoreCase(category)) {
            return TRANSACTION;
        }
        return List.of();
    }

    /**
     * Sample vars for live preview (OTP or TRANSACTION).
     */
    public static Map<String, String> sampleVars(String category) {
        Map<String, String> vars = new LinkedHashMap<>();
        if (SmsTemplateKeys.CATEGORY_OTP.equalsIgnoreCase(category)) {
            vars.put("otp", "123456");
            vars.put("purpose_label", "verification");
            vars.put("valid_minutes", "5");
            return vars;
        }
        vars.put("amount", "100.00");
        vars.put("currency", "KES");
        vars.put("counterparty", "ACME TILL");
        vars.put("counterparty_suffix", " to ACME TILL");
        vars.put("to_account", "****5678");
        vars.put("from_account", "****1234");
        vars.put("ref", "UTRANS123");
        vars.put("vyce_ref", "vyce-tx-001");
        vars.put("channel", "PAY_TILL");
        vars.put("channel_label", "Till payment");
        vars.put("date", "28-09-2026");
        vars.put("time", "14:30");
        vars.put("status_label", "Confirmed");
        vars.put("error_msg", "Insufficient funds");
        vars.put("error_suffix", " Insufficient funds.");
        return vars;
    }
}
