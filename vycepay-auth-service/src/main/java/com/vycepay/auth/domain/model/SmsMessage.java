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
 * Outbound SMS ledger row. Maps to sms_message.
 */
@Entity
@Table(name = "sms_message")
public class SmsMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, length = 36)
    private String publicId;

    @Column(name = "batch_id", length = 36)
    private String batchId;

    @Column(name = "recipient", nullable = false, length = 20)
    private String recipient;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "purpose", nullable = false, length = 32)
    private String purpose;

    @Column(name = "otp_purpose", length = 32)
    private String otpPurpose;

    @Column(name = "otp_verification_id")
    private Long otpVerificationId;

    @Column(name = "message_body", nullable = false, length = 640)
    private String messageBody;

    @Column(name = "message_redacted", nullable = false, length = 640)
    private String messageRedacted;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider = "MOBIWAVE";

    @Column(name = "provider_uid", length = 64)
    private String providerUid;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getOtpPurpose() { return otpPurpose; }
    public void setOtpPurpose(String otpPurpose) { this.otpPurpose = otpPurpose; }
    public Long getOtpVerificationId() { return otpVerificationId; }
    public void setOtpVerificationId(Long otpVerificationId) { this.otpVerificationId = otpVerificationId; }
    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }
    public String getMessageRedacted() { return messageRedacted; }
    public void setMessageRedacted(String messageRedacted) { this.messageRedacted = messageRedacted; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderUid() { return providerUid; }
    public void setProviderUid(String providerUid) { this.providerUid = providerUid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(Long createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
