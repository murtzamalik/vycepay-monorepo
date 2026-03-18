package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Request for send money (transfer).
 */
@Schema(description = "Transfer/send money request")
public class SendMoneyRequest {

    @Schema(description = "Payee bank code (e.g. M-PESA)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payeeBankCode;

    @Schema(description = "Recipient account/M-PESA number", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payeeAccountId;

    @Schema(description = "Recipient name (optional)")
    private String payeeAccountName;

    @Schema(description = "Amount in KES", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Optional remark")
    private String remark;

    public String getPayeeBankCode() {
        return payeeBankCode;
    }

    public void setPayeeBankCode(String payeeBankCode) {
        this.payeeBankCode = payeeBankCode;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
