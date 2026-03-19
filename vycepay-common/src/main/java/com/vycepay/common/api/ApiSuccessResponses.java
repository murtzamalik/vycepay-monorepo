package com.vycepay.common.api;

import org.slf4j.MDC;

/**
 * Factory methods for standard success envelopes.
 */
public final class ApiSuccessResponses {

    private ApiSuccessResponses() {
    }

    public static ApiSuccessResponse<Void> ok(String code, String message) {
        return new ApiSuccessResponse<>(true, code, message, MDC.get("requestId"), null);
    }

    public static <T> ApiSuccessResponse<T> ok(String code, String message, T data) {
        return new ApiSuccessResponse<>(true, code, message, MDC.get("requestId"), data);
    }
}
