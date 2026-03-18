package com.vycepay.common.choicebank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Choice Bank API request envelope.
 * All outbound requests must include: requestId, sender, locale, timestamp, salt, signature, params.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChoiceBankRequest {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("sender")
    private String sender;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("salt")
    private String salt;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("params")
    private Map<String, Object> params;

    public static ChoiceBankRequestBuilder builder() {
        return new ChoiceBankRequestBuilder();
    }

    public static class ChoiceBankRequestBuilder {
        private final ChoiceBankRequest request = new ChoiceBankRequest();

        public ChoiceBankRequestBuilder requestId(String requestId) {
            request.setRequestId(requestId);
            return this;
        }

        public ChoiceBankRequestBuilder sender(String sender) {
            request.setSender(sender);
            return this;
        }

        public ChoiceBankRequestBuilder locale(String locale) {
            request.setLocale(locale);
            return this;
        }

        public ChoiceBankRequestBuilder timestamp(Long timestamp) {
            request.setTimestamp(timestamp);
            return this;
        }

        public ChoiceBankRequestBuilder salt(String salt) {
            request.setSalt(salt);
            return this;
        }

        public ChoiceBankRequestBuilder signature(String signature) {
            request.setSignature(signature);
            return this;
        }

        public ChoiceBankRequestBuilder params(Map<String, Object> params) {
            request.setParams(params);
            return this;
        }

        public ChoiceBankRequest build() {
            return request;
        }
    }

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

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
