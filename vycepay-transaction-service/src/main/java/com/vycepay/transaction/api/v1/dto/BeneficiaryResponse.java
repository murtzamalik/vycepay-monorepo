package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Saved beneficiary for list / create / update responses.
 */
@Schema(description = "Beneficiary")
public class BeneficiaryResponse {

    @Schema(description = "Public UUID")
    private String externalId;

    @Schema(description = "User nickname")
    private String nickname;

    @Schema(description = "Rail type 0–5")
    private int accountType;

    @Schema(description = "Bank / rail code (may be empty)")
    private String payeeBankCode;

    @Schema(description = "Account / mobile / paybill / till")
    private String payeeAccountId;

    @Schema(description = "Last known account title")
    private String payeeAccountName;

    public BeneficiaryResponse() {
    }

    public BeneficiaryResponse(String externalId, String nickname, int accountType,
                               String payeeBankCode, String payeeAccountId, String payeeAccountName) {
        this.externalId = externalId;
        this.nickname = nickname;
        this.accountType = accountType;
        this.payeeBankCode = payeeBankCode;
        this.payeeAccountId = payeeAccountId;
        this.payeeAccountName = payeeAccountName;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getAccountType() {
        return accountType;
    }

    public void setAccountType(int accountType) {
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
