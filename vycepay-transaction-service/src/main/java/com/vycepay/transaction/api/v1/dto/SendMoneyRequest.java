package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Request for send money (transfer).
 */
@Schema(description = "Transfer/send money request")
public class SendMoneyRequest {

    @Schema(description = "Payee bank code (e.g. M-PESA or PesaLink bank code from GET /bank-codes)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String payeeBankCode;

    @Schema(description = "Recipient account/M-PESA number", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payeeAccountId;

    @Schema(description = "Counterparty type for Hakikisha re-validation: 0=Choice, 1=Paybill, 2=Till, 3=M-Pesa mobile, 4=PesaLink, 5=IMT",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer accountType;

    @Schema(description = "Optional display hint; server overwrites with Choice accountName from validateAccount")
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

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
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
