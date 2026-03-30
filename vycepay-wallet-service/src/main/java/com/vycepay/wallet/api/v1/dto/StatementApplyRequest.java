package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request for periodic account statement (statement/applyAccountStatement).
 */
public class StatementApplyRequest {

    @Schema(description = "Statement period start (Unix ms)")
    private long statementStartTime;

    @Schema(description = "Statement period end (Unix ms)")
    private long statementEndTime;

    @Schema(description = "Optional file type per Choice (e.g. PDF/CSV code)")
    private Integer fileType;

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
