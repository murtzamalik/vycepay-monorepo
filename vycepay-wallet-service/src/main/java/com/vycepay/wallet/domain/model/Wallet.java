package com.vycepay.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Wallet/account mapping. Maps to wallet table.
 * balance_cache updated by callback 0003.
 */
@Entity
@Table(name = "wallet")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "choice_account_id", nullable = false, unique = true)
    private String choiceAccountId;

    @Column(name = "choice_account_type")
    private String choiceAccountType;

    @Column(name = "balance_cache", precision = 18, scale = 2)
    private BigDecimal balanceCache = BigDecimal.ZERO;

    @Column(name = "currency")
    private String currency;

    @Column(name = "status")
    private String status;

    @Column(name = "last_balance_update_at")
    private Instant lastBalanceUpdateAt;

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

    public String getChoiceAccountType() {
        return choiceAccountType;
    }

    public void setChoiceAccountType(String choiceAccountType) {
        this.choiceAccountType = choiceAccountType;
    }

    public BigDecimal getBalanceCache() {
        return balanceCache;
    }

    public void setBalanceCache(BigDecimal balanceCache) {
        this.balanceCache = balanceCache;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastBalanceUpdateAt() {
        return lastBalanceUpdateAt;
    }

    public void setLastBalanceUpdateAt(Instant lastBalanceUpdateAt) {
        this.lastBalanceUpdateAt = lastBalanceUpdateAt;
    }
}
