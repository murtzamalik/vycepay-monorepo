package com.vycepay.callback.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * In-app notification inbox row. Source of truth for what the customer should see.
 */
@Entity
@Table(name = "customer_notification")
public class CustomerNotification {

    public static final String SOURCE_CALLBACK = "CALLBACK";
    public static final String SOURCE_ADMIN_COMPOSE = "ADMIN_COMPOSE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Matches Flyway V9 {@code CHAR(36)} — Hibernate defaults String→VARCHAR. */
    @Column(name = "public_id", nullable = false, length = 36, unique = true, columnDefinition = "CHAR(36)")
    private String publicId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "push_type", nullable = false, length = 64)
    private String pushType;

    @Column(name = "notification_type", length = 8)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "body", nullable = false, length = 512)
    private String body;

    @Column(name = "data_json", columnDefinition = "JSON")
    private String dataJson;

    @Column(name = "choice_callback_id")
    private Long choiceCallbackId;

    /**
     * Money-event dedupe key (e.g. TX:{choiceTxId}). Null for KYC/statement/admin compose.
     * Unique with customer_id (Flyway V10).
     */
    @Column(name = "dedupe_key", length = 160)
    private String dedupeKey;

    /** Matches Flyway V9 {@code CHAR(36)} — Hibernate defaults String→VARCHAR. */
    @Column(name = "batch_id", length = 36, columnDefinition = "CHAR(36)")
    private String batchId;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPushType() {
        return pushType;
    }

    public void setPushType(String pushType) {
        this.pushType = pushType;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public Long getChoiceCallbackId() {
        return choiceCallbackId;
    }

    public void setChoiceCallbackId(Long choiceCallbackId) {
        this.choiceCallbackId = choiceCallbackId;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Long getCreatedByAdminId() {
        return createdByAdminId;
    }

    public void setCreatedByAdminId(Long createdByAdminId) {
        this.createdByAdminId = createdByAdminId;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
