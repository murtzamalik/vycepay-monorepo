package com.vycepay.callback.application.notification;

import com.vycepay.callback.domain.model.PushMessage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds SMS template variables from a TRANSACTION_RESULT {@link PushMessage}.
 */
public final class TxSmsVariableFactory {

    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final int TX_STATUS_SUCCESS = 8;

    private TxSmsVariableFactory() {
    }

    public static Map<String, String> fromPushMessage(PushMessage message) {
        Map<String, String> data = message != null && message.getData() != null
                ? message.getData() : Map.of();
        String amountRaw = data.get("amount");
        String currency = firstNonBlank(data.get("currency"), "KES");
        String amount = absoluteAmount(amountRaw);
        boolean outbound = isOutbound(amountRaw);
        String counterparty = blankToNull(data.get("counterparty"));
        String channel = data.get("paymentChannel");

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("amount", amount);
        vars.put("currency", currency);
        vars.put("counterparty", nullToEmpty(counterparty));
        vars.put("counterparty_suffix", counterpartySuffix(outbound, counterparty));
        vars.put("from_account", nullToEmpty(data.get("fromAccount")));
        vars.put("to_account", nullToEmpty(data.get("toAccount")));
        vars.put("ref", firstNonBlank(data.get("externalTxId"), data.get("reference"), data.get("txId"), ""));
        vars.put("vyce_ref", nullToEmpty(data.get("externalId")));
        vars.put("channel", nullToEmpty(channel));
        vars.put("channel_label", channelLabel(channel));
        vars.put("status_label", isSuccess(data.get("txStatus")) ? "Confirmed" : "Failed");
        vars.put("error_msg", nullToEmpty(data.get("errorMsg")));
        String err = blankToNull(data.get("errorMsg"));
        vars.put("error_suffix", err != null ? " " + err + "." : "");
        fillDateTime(vars, data.get("completedAt"));
        return vars;
    }

    public static boolean isOutboundAmount(String amountRaw) {
        return isOutbound(amountRaw);
    }

    public static boolean isSuccessStatus(String txStatus) {
        return isSuccess(txStatus);
    }

    private static void fillDateTime(Map<String, String> vars, String completedAtIso) {
        Instant instant = null;
        if (completedAtIso != null && !completedAtIso.isBlank()) {
            try {
                instant = Instant.parse(completedAtIso);
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (instant == null) {
            instant = Instant.now();
        }
        var zoned = instant.atZone(NAIROBI);
        vars.put("date", DATE_FMT.format(zoned));
        vars.put("time", TIME_FMT.format(zoned));
    }

    private static String counterpartySuffix(boolean outbound, String counterparty) {
        if (counterparty == null || counterparty.isBlank()) {
            return "";
        }
        return (outbound ? " to " : " from ") + counterparty;
    }

    private static String channelLabel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "Payment";
        }
        return switch (channel.trim().toUpperCase(Locale.ROOT)) {
            case "PAY_TILL" -> "Till payment";
            case "PAY_BILL" -> "Bill payment";
            case "MOBILE_MONEY" -> "Mobile money";
            case "INTERNAL_TRANSFER" -> "Transfer";
            default -> "Payment";
        };
    }

    private static boolean isSuccess(String txStatus) {
        if (txStatus == null || txStatus.isBlank()) {
            return true;
        }
        try {
            return Integer.parseInt(txStatus.trim()) == TX_STATUS_SUCCESS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static boolean isOutbound(String amount) {
        if (amount == null || amount.isBlank()) {
            return false;
        }
        try {
            return Double.parseDouble(amount.trim()) < 0;
        } catch (NumberFormatException e) {
            return amount.trim().startsWith("-");
        }
    }

    private static String absoluteAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return "";
        }
        String normalized = amount.trim().startsWith("-") ? amount.trim().substring(1) : amount.trim();
        return normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String s : values) {
            if (s != null && !s.isBlank() && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        return "";
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }

    private static String nullToEmpty(String s) {
        return s != null && !"null".equalsIgnoreCase(s) ? s : "";
    }
}
