package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request for account statement emailed by Choice ({@code statement/applyBankAccountStatement}).
 */
public class StatementApplyRequest {

    @Schema(description = "Registered email for the statement (required). Prefill from GET /wallets/account/details email; Choice delivers to registered address only.",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "customer@example.com")
    private String email;

    @Schema(description = "Statement period start (Unix ms)", requiredMode = Schema.RequiredMode.REQUIRED)
    private long statementStartTime;

    @Schema(description = "Statement period end (Unix ms); max 180 days after start",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private long statementEndTime;

    @Schema(description = "Optional file type: 0=PDF, 1=Excel (mapped to Choice pdf/xlsx). Choice email flow is primarily Excel.")
    private Integer fileType;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getStatementStartTime() {
        return statementStartTime;
    }

    public void setStatementStartTime(long statementStartTime) {
        this.statementStartTime = statementStartTime;
    }

    public long getStatementEndTime() {
        return statementEndTime;
    }

    public void setStatementEndTime(long statementEndTime) {
        this.statementEndTime = statementEndTime;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }
}
