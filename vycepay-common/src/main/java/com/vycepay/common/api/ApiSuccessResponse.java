package com.vycepay.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard success envelope for action endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiSuccessResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private String requestId;
    private T data;

    public ApiSuccessResponse() {
    }

    public ApiSuccessResponse(boolean success, String code, String message, String requestId, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
