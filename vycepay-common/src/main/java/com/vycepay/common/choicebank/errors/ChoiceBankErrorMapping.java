package com.vycepay.common.choicebank.errors;

/**
 * Resolved mapping for a Choice Bank response code (client-facing code and HTTP status).
 */
public final class ChoiceBankErrorMapping {

    private final String clientCode;
    private final int httpStatus;
    private final boolean retryable;
    private final String category;

    public ChoiceBankErrorMapping(String clientCode, int httpStatus, boolean retryable, String category) {
        this.clientCode = clientCode;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
        this.category = category;
    }

    public String getClientCode() {
        return clientCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getCategory() {
        return category;
    }
}
