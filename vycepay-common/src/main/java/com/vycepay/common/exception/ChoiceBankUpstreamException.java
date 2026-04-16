package com.vycepay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Choice Bank BaaS returned a non-success code. Carries upstream metadata for API clients and support.
 */
public class ChoiceBankUpstreamException extends BusinessException {

    private final String choiceCode;
    private final String choiceMessage;
    private final String choiceRequestId;
    private final String choicePath;
    private final boolean retryable;

    public ChoiceBankUpstreamException(
            String clientCode,
            String message,
            HttpStatus httpStatus,
            String choiceCode,
            String choiceMessage,
            String choiceRequestId,
            String choicePath,
            boolean retryable) {
        super(clientCode, message, httpStatus);
        this.choiceCode = choiceCode;
        this.choiceMessage = choiceMessage;
        this.choiceRequestId = choiceRequestId;
        this.choicePath = choicePath;
        this.retryable = retryable;
    }

    public String getChoiceCode() {
        return choiceCode;
    }

    public String getChoiceMessage() {
        return choiceMessage;
    }

    public String getChoiceRequestId() {
        return choiceRequestId;
    }

    public String getChoicePath() {
        return choicePath;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
