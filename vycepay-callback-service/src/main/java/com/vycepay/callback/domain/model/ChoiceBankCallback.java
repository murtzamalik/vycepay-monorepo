package com.vycepay.callback.domain.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Persisted Choice Bank callback for audit and idempotency.
 * Unique on (choiceRequestId, notificationType) to handle duplicate callbacks.
 */
@Entity
@Table(name = "choice_bank_callback",
        uniqueConstraints = @UniqueConstraint(columnNames = {"choice_request_id", "notification_type"}))
public class ChoiceBankCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "choice_request_id")
    private String choiceRequestId;

    @Column(name = "notification_type", nullable = false, length = 8)
    private String notificationType;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "LONGTEXT")
    private String rawPayload;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChoiceRequestId() {
        return choiceRequestId;
    }

    public void setChoiceRequestId(String choiceRequestId) {
        this.choiceRequestId = choiceRequestId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static ChoiceBankCallbackBuilder builder() {
        return new ChoiceBankCallbackBuilder();
    }

    public static class ChoiceBankCallbackBuilder {
        private final ChoiceBankCallback entity = new ChoiceBankCallback();

        public ChoiceBankCallbackBuilder choiceRequestId(String choiceRequestId) {
            entity.setChoiceRequestId(choiceRequestId);
            return this;
        }

        public ChoiceBankCallbackBuilder notificationType(String notificationType) {
            entity.setNotificationType(notificationType);
            return this;
        }

        public ChoiceBankCallbackBuilder rawPayload(String rawPayload) {
            entity.setRawPayload(rawPayload);
            return this;
        }

        public ChoiceBankCallbackBuilder processed(Boolean processed) {
            entity.setProcessed(processed != null ? processed : false);
            return this;
        }

        public ChoiceBankCallback build() {
            return entity;
        }
    }
}
