package com.vycepay.common.choicebank.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Choice Bank callback/webhook payload structure.
 * All callbacks share: requestId, sender, locale, timestamp, notificationType, params, salt, signature.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChoiceBankCallbackPayload {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("sender")
    private String sender;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("notificationType")
    private String notificationType;

    @JsonProperty("params")
    private Map<String, Object> params;

    @JsonProperty("salt")
    private String salt;

    @JsonProperty("signature")
    private String signature;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
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
}
