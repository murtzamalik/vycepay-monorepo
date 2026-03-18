package com.vycepay.common.choicebank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice Bank API response envelope.
 * code "00000" indicates success; data contains the response payload.
 */
public class ChoiceBankResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("sender")
    private String sender;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("salt")
    private String salt;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("data")
    private Object data;

    /**
     * @return true if the request completed successfully
     */
    public boolean isSuccess() {
        return "00000".equals(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
