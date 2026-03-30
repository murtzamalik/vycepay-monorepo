package com.vycepay.wallet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Read-only slice of kyc_verification for Choice userId resolution. Same table as KYC service.
 */
@Entity
@Table(name = "kyc_verification")
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "choice_user_id")
    private String choiceUserId;

    @Column(name = "choice_account_id")
    private String choiceAccountId;

    @Column(name = "status", nullable = false)
    private String status;

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

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getChoiceUserId() {
        return choiceUserId;
    }

    public void setChoiceUserId(String choiceUserId) {
        this.choiceUserId = choiceUserId;
    }

    public String getChoiceAccountId() {
        return choiceAccountId;
    }

    public void setChoiceAccountId(String choiceAccountId) {
        this.choiceAccountId = choiceAccountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
