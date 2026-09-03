package com.vycepay.common.sms.port;

/**
 * Outcome of an SMS send attempt.
 *
 * @param status        SENT, FAILED, or SKIPPED
 * @param providerUid   provider message UID when available
 * @param errorMessage  human-readable failure/skip reason
 */
public record SmsSendResult(String status, String providerUid, String errorMessage) {

    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    public static final String SKIPPED = "SKIPPED";

    public static SmsSendResult sent(String providerUid) {
        return new SmsSendResult(SENT, providerUid, null);
    }

    public static SmsSendResult failed(String errorMessage) {
        return new SmsSendResult(FAILED, null, truncate(errorMessage));
    }

    public static SmsSendResult skipped(String reason) {
        return new SmsSendResult(SKIPPED, null, truncate(reason));
    }

    public boolean isSent() {
        return SENT.equals(status);
    }

    private static String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() <= 255 ? msg : msg.substring(0, 255);
    }
}
