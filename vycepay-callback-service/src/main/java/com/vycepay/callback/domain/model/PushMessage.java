package com.vycepay.callback.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable push payload sent to a customer's registered FCM devices.
 * {@code data} values must be strings (FCM data message requirement).
 */
public final class PushMessage {

    private final String title;
    private final String body;
    private final String pushType;
    private final String notificationType;
    private final Map<String, String> data;

    private PushMessage(Builder builder) {
        this.title = builder.title;
        this.body = builder.body;
        this.pushType = builder.pushType;
        this.notificationType = builder.notificationType;
        Map<String, String> copy = new LinkedHashMap<>();
        if (builder.data != null) {
            copy.putAll(builder.data);
        }
        if (builder.pushType != null) {
            copy.putIfAbsent("pushType", builder.pushType);
        }
        if (builder.notificationType != null) {
            copy.putIfAbsent("notificationType", builder.notificationType);
        }
        this.data = Collections.unmodifiableMap(copy);
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getPushType() {
        return pushType;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public Map<String, String> getData() {
        return data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private String body;
        private String pushType;
        private String notificationType;
        private Map<String, String> data = new LinkedHashMap<>();

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder pushType(String pushType) {
            this.pushType = pushType;
            return this;
        }

        public Builder notificationType(String notificationType) {
            this.notificationType = notificationType;
            return this;
        }

        public Builder putData(String key, String value) {
            if (key != null && value != null && !value.isBlank()) {
                this.data.put(key, value);
            }
            return this;
        }

        public PushMessage build() {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(pushType, "pushType");
            Objects.requireNonNull(notificationType, "notificationType");
            return new PushMessage(this);
        }
    }
}
