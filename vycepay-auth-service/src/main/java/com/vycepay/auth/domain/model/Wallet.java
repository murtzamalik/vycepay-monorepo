package com.vycepay.auth.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only wallet reference for auth-service contact matching.
 * Maps to shared {@code wallet} table; created when Choice onboarding status=7.
 */
@Entity
@Table(name = "wallet")
public class Wallet {

    public static final String STATUS_ACTIVE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "choice_account_id", nullable = false, unique = true)
    private String choiceAccountId;

    @Column(name = "status")
    private String status;

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
}
