package com.vycepay.common.choicebank.dto;

import java.time.Instant;

/**
 * One recorded outbound Choice BaaS HTTP exchange (sanitized payloads).
 * In-memory only; each JVM (KYC, Transaction, Wallet) has its own buffer.
 */
public class ChoiceBankHttpTraceDto {

    /** When the call completed or failed. */
    private Instant timestamp;

    /** Per-call request id (correlates with choice_baas_* logs). */
    private String choiceBankRequestId;

    /** API path, e.g. trans/v2/applyForTransfer. */
    private String path;

    /** Sanitized outbound request JSON. */
    private String requestPayload;

    /** Sanitized raw response JSON when available. */
    private String responsePayload;

    /** Choice response code when parsed. */
    private String responseCode;

    /** Choice response message when parsed. */
    private String responseMsg;

    /** Set when HTTP or parse failed. */
    private String error;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getChoiceBankRequestId() {
        return choiceBankRequestId;
    }

    public void setChoiceBankRequestId(String choiceBankRequestId) {
        this.choiceBankRequestId = choiceBankRequestId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
