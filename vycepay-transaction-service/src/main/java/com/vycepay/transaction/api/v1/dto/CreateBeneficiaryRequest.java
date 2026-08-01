package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to create or upsert a saved beneficiary.
 */
@Schema(description = "Create / upsert beneficiary")
public class CreateBeneficiaryRequest {

    @Schema(description = "Display nickname (1–50 chars)", requiredMode = Schema.RequiredMode.REQUIRED, example = "Mum Equity")
    private String nickname;

    @Schema(description = "0=Choice, 1=Paybill, 2=Till, 3=M-Pesa mobile, 4=PesaLink, 5=IMT",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer accountType;

    @Schema(description = "Bank / rail code; required when accountType is 4. Empty for rails without bank code.")
    private String payeeBankCode;

    @Schema(description = "Account, mobile, paybill, or till", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payeeAccountId;

    @Schema(description = "Last known title from Choice validate (optional but recommended)")
    private String payeeAccountName;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

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
}
