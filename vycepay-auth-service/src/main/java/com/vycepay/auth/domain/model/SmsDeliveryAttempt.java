package com.vycepay.auth.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One SMS provider send attempt. Maps to sms_delivery_attempt.
 */
@Entity
@Table(name = "sms_delivery_attempt")
public class SmsDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sms_message_id", nullable = false)
    private Long smsMessageId;

    @Column(name = "trigger_source", nullable = false, length = 32)
    private String triggerSource;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "provider_uid", length = 64)
    private String providerUid;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSmsMessageId() { return smsMessageId; }
    public void setSmsMessageId(Long smsMessageId) { this.smsMessageId = smsMessageId; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProviderUid() { return providerUid; }
    public void setProviderUid(String providerUid) { this.providerUid = providerUid; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(Long createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
