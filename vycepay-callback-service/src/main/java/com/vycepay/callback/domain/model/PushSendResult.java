package com.vycepay.callback.domain.model;

/**
 * Outcome of an FCM send attempt for delivery logging.
 */
public final class PushSendResult {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    public static final String SKIP_FIREBASE_DISABLED = "FIREBASE_DISABLED";
    public static final String SKIP_NO_TOKENS = "NO_TOKENS";
    public static final String SKIP_NO_CUSTOMER = "NO_CUSTOMER";
    public static final String SKIP_UNSUPPORTED_TYPE = "UNSUPPORTED_TYPE";
    /** Same Choice txId already has a TRANSACTION_RESULT inbox row (0002/0003 pair). */
    public static final String SKIP_ALREADY_NOTIFIED = "ALREADY_NOTIFIED";

    private final String status;
    private final String skipReason;
    private final int tokenCount;
    private final int successCount;
    private final int failureCount;
    private final String errorMessage;

    private PushSendResult(String status, String skipReason, int tokenCount,
                           int successCount, int failureCount, String errorMessage) {
        this.status = status;
        this.skipReason = skipReason;
        this.tokenCount = tokenCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errorMessage = errorMessage;
    }

    public static PushSendResult skipped(String skipReason) {
        return new PushSendResult(STATUS_SKIPPED, skipReason, 0, 0, 0, null);
    }

    public static PushSendResult sent(int tokenCount, int successCount, int failureCount) {
        String status;
        if (failureCount == 0) {
            status = STATUS_SENT;
        } else if (successCount == 0) {
            status = STATUS_FAILED;
        } else {
            status = STATUS_PARTIAL;
        }
        return new PushSendResult(status, null, tokenCount, successCount, failureCount, null);
    }

    public static PushSendResult failed(String errorMessage) {
        String msg = errorMessage;
        if (msg != null && msg.length() > 255) {
            msg = msg.substring(0, 255);
        }
        return new PushSendResult(STATUS_FAILED, null, 0, 0, 0, msg);
    }

    public String getStatus() {
        return status;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
