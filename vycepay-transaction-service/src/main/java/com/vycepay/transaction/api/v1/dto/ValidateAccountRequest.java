package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request for Choice Bank Hakikisha account validation (title fetch) before transfer.
 */
@Schema(description = "Validate recipient account / fetch account title (Hakikisha)")
public class ValidateAccountRequest {

    @Schema(description = "Account, mobile, paybill, or till to validate", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountId;

    @Schema(description = "Counterparty type: 0=Choice, 1=Paybill, 2=Till, 3=M-Pesa mobile, 4=PesaLink, 5=IMT",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer accountType;

    @Schema(description = "Bank code from GET /bank-codes; mandatory when accountType is 4 (PesaLink)")
    private String bankCode;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }
}
