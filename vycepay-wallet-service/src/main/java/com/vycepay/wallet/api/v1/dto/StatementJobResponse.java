package com.vycepay.wallet.api.v1.dto;

import com.vycepay.wallet.domain.model.AccountStatementJob;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Local statement job row. Email-delivery jobs have {@code email} and no {@code downloadUrl}.
 */
public class StatementJobResponse {

    private String choiceRequestId;
    private String accountId;
    private String status;
    @Schema(description = "Destination email used on apply; null for legacy URL jobs")
    private String email;
    @Schema(description = "Download URL for legacy URL-flow jobs only; null for email-delivery jobs")
    private String downloadUrl;
    private String fileName;
    private String errorMsg;
    private Instant createdAt;
    private Instant updatedAt;

    public static StatementJobResponse from(AccountStatementJob j) {
        StatementJobResponse r = new StatementJobResponse();
        r.choiceRequestId = j.getChoiceRequestId();
        r.accountId = j.getAccountId();
        r.status = j.getStatus();
        r.email = j.getEmail();
        r.downloadUrl = j.getDownloadUrl();
        r.fileName = j.getFileName();
        r.errorMsg = j.getErrorMsg();
        r.createdAt = j.getCreatedAt();
        r.updatedAt = j.getUpdatedAt();
        return r;
    }

    public String getChoiceRequestId() {
        return choiceRequestId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
