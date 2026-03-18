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
}
