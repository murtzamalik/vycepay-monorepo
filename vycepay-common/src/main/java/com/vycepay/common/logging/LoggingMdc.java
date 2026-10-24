package com.vycepay.common.logging;

import org.slf4j.MDC;

/**
 * Helpers to put/clear structured logging fields in MDC for JSON Logback output.
 */
public final class LoggingMdc {

    private LoggingMdc() {
    }

    public static void put(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (value == null || value.isBlank()) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    public static void putEvent(String dataset, String action, String outcome) {
        put(LoggingMdcKeys.EVENT_DATASET, dataset);
        put(LoggingMdcKeys.EVENT_ACTION, action);
        put(LoggingMdcKeys.EVENT_OUTCOME, outcome);
    }

    public static void putError(String code, String type, String message) {
        put(LoggingMdcKeys.ERROR_CODE, code);
        put(LoggingMdcKeys.ERROR_TYPE, type);
        put(LoggingMdcKeys.ERROR_MESSAGE, truncate(message, 500));
    }

    /**
     * Clears keys that are typically call scoped (not requestId owned by the filter).
     */
    public static void clearEventFields() {
        MDC.remove(LoggingMdcKeys.EVENT_DATASET);
        MDC.remove(LoggingMdcKeys.EVENT_ACTION);
        MDC.remove(LoggingMdcKeys.EVENT_OUTCOME);
        MDC.remove(LoggingMdcKeys.ERROR_CODE);
        MDC.remove(LoggingMdcKeys.ERROR_TYPE);
        MDC.remove(LoggingMdcKeys.ERROR_MESSAGE);
        MDC.remove(LoggingMdcKeys.CHOICE_BANK_REQUEST_ID);
        MDC.remove(LoggingMdcKeys.CHOICE_PATH);
        MDC.remove(LoggingMdcKeys.CHOICE_CODE);
        MDC.remove(LoggingMdcKeys.CHOICE_MSG);
        MDC.remove(LoggingMdcKeys.LABELS_FLOW);
        MDC.remove(LoggingMdcKeys.ONBOARDING_REQUEST_ID);
        MDC.remove(LoggingMdcKeys.TRANSACTION_ID);
        MDC.remove(LoggingMdcKeys.CHOICE_TX_ID);
        MDC.remove(LoggingMdcKeys.IDEMPOTENCY_KEY);
        MDC.remove(LoggingMdcKeys.HTTP_METHOD);
        MDC.remove(LoggingMdcKeys.URL_PATH);
    }

    public static void clearCustomerAndDomainIds() {
        MDC.remove(LoggingMdcKeys.CUSTOMER_ID);
        MDC.remove(LoggingMdcKeys.ONBOARDING_REQUEST_ID);
        MDC.remove(LoggingMdcKeys.TRANSACTION_ID);
        MDC.remove(LoggingMdcKeys.CHOICE_TX_ID);
        MDC.remove(LoggingMdcKeys.IDEMPOTENCY_KEY);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
