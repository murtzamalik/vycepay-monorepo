package com.vycepay.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Consistent error envelope per API contracts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String code;
    private String message;
    private String requestId;
    private Object details;

    /** Choice BaaS response.code when failure originated upstream (optional). */
    private String choiceCode;
    /** Choice correlation id (requestId in BaaS envelope). */
    private String choiceRequestId;
    /** Choice API path that was invoked (e.g. trans/v2/applyForTransfer). */
    private String choicePath;
    /** Whether the catalog marks this failure as safe to retry. */
    private Boolean retryable;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message, String requestId, Object details) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    public String getChoiceCode() {
        return choiceCode;
    }

    public void setChoiceCode(String choiceCode) {
        this.choiceCode = choiceCode;
    }

    public String getChoiceRequestId() {
        return choiceRequestId;
    }

    public void setChoiceRequestId(String choiceRequestId) {
        this.choiceRequestId = choiceRequestId;
    }

    public String getChoicePath() {
        return choicePath;
    }

    public void setChoicePath(String choicePath) {
        this.choicePath = choicePath;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }
}
