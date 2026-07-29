package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response from Choice Bank Hakikisha account validation (title fetch).
 */
@Schema(description = "Validated recipient account details from Choice Bank")
public class ValidateAccountResponse {

    @Schema(description = "Account id that was validated")
    private String accountId;

    @Schema(description = "Counterparty account type")
    private Integer accountType;

    @Schema(description = "Account holder name from Choice Bank")
    private String accountName;

    @Schema(description = "0=normal, 1=frozen")
    private Integer freezeStatus;

    @Schema(description = "0=normal, 1=restrict in, 2=restrict out")
    private Integer restrictStatus;

    @Schema(description = "True when account can receive funds (not frozen and not restrict-in)")
    private boolean valid;

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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Integer getFreezeStatus() {
        return freezeStatus;
    }

    public void setFreezeStatus(Integer freezeStatus) {
        this.freezeStatus = freezeStatus;
    }

    public Integer getRestrictStatus() {
        return restrictStatus;
    }

    public void setRestrictStatus(Integer restrictStatus) {
        this.restrictStatus = restrictStatus;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
