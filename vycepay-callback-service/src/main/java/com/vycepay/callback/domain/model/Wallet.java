package com.vycepay.callback.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wallet/account mapping - maps to wallet table.
 * Updated by callbacks 0001 (create when account opened) and 0003 (balance change).
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

    public static WalletBuilder builder() {
        return new WalletBuilder();
    }

    public static class WalletBuilder {
        private final Wallet entity = new Wallet();

        public WalletBuilder customerId(Long customerId) {
            entity.setCustomerId(customerId);
            return this;
        }

        public WalletBuilder choiceAccountId(String choiceAccountId) {
            entity.setChoiceAccountId(choiceAccountId);
            return this;
        }

        public WalletBuilder choiceAccountType(String choiceAccountType) {
            entity.setChoiceAccountType(choiceAccountType);
            return this;
        }

        public WalletBuilder balanceCache(BigDecimal balanceCache) {
            entity.setBalanceCache(balanceCache != null ? balanceCache : BigDecimal.ZERO);
            return this;
        }

        public WalletBuilder currency(String currency) {
            entity.setCurrency(currency);
            return this;
        }

        public WalletBuilder status(String status) {
            entity.setStatus(status);
            return this;
        }

        public Wallet build() {
            return entity;
        }
    }
}
