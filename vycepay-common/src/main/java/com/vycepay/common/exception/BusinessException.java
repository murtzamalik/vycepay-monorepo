package com.vycepay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain exception with explicit code and HTTP status.
 * Use for business rule violations; map to ErrorResponse in GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    /**
     * Creates exception with code and message. Defaults to BAD_REQUEST (400).
     */
    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Creates exception with code, message, and HTTP status.
     */
    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /**
     * Creates exception with cause for logging; code and message from parameters.
     */
    public BusinessException(String code, String message, Throwable cause) {
        this(code, message, HttpStatus.BAD_REQUEST, cause);
    }

    public BusinessException(String code, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
