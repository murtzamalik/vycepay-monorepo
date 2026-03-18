package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Wallet response for API.
 */
@Schema(description = "Wallet/account information")
public class WalletResponse {

    @Schema(description = "Choice Bank account ID")
    private String choiceAccountId;

    @Schema(description = "Cached balance (Choice Bank is source of truth)")
    private BigDecimal balance;

    @Schema(description = "Currency (KES)")
    private String currency;

    @Schema(description = "Status (ACTIVE, SUSPENDED)")
    private String status;

    public WalletResponse() {
    }

    public WalletResponse(String choiceAccountId, BigDecimal balance, String currency, String status) {
        this.choiceAccountId = choiceAccountId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
    }

    public String getChoiceAccountId() {
        return choiceAccountId;
    }

    public void setChoiceAccountId(String choiceAccountId) {
        this.choiceAccountId = choiceAccountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
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
}
