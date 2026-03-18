package com.vycepay.transaction.api.v1.dto;

import com.vycepay.transaction.domain.model.TransactionDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction response for API.
 */
@Schema(description = "Transaction record")
public class TransactionResponse {

    @Schema(description = "External transaction ID (UUID)")
    private String externalId;

    @Schema(description = "Transaction type (TRANSFER, DEPOSIT)")
    private String type;

    @Schema(description = "Amount")
    private BigDecimal amount;

    @Schema(description = "Currency (e.g. KES)")
    private String currency;

    @Schema(description = "Raw status code from Choice Bank (1=PENDING, 2=PROCESSING, 4=FAILED, 8=SUCCESS)")
    private String status;

    @Schema(description = "Human-readable status (PENDING, PROCESSING, FAILED, SUCCESS)")
    private TransactionDisplayStatus displayStatus;

    @Schema(description = "Payee account ID or M-PESA number")
    private String payeeAccountId;

    @Schema(description = "Payee account name (optional)")
    private String payeeAccountName;

    @Schema(description = "Payee bank code")
    private String payeeBankCode;

    @Schema(description = "Remark / reference note")
    private String remark;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Completion timestamp")
    private Instant completedAt;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public TransactionDisplayStatus getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(TransactionDisplayStatus displayStatus) {
        this.displayStatus = displayStatus;
    }

    public String getPayeeAccountId() {
        return payeeAccountId;
    }

    public void setPayeeAccountId(String payeeAccountId) {
        this.payeeAccountId = payeeAccountId;
    }

    public String getPayeeAccountName() {
        return payeeAccountName;
    }

    public void setPayeeAccountName(String payeeAccountName) {
        this.payeeAccountName = payeeAccountName;
    }

    public String getPayeeBankCode() {
        return payeeBankCode;
    }

    public void setPayeeBankCode(String payeeBankCode) {
        this.payeeBankCode = payeeBankCode;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
