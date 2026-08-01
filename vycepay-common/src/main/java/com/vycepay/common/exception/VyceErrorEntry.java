package com.vycepay.common.exception;

/**
 * Resolved entry from the VycePay error catalog.
 */
public final class VyceErrorEntry {

    private final String code;
    private final int httpStatus;
    private final String userMessage;
    private final boolean retryable;

    public VyceErrorEntry(String code, int httpStatus, String userMessage, boolean retryable) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.userMessage = userMessage;
        this.retryable = retryable;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
