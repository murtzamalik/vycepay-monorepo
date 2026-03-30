package com.vycepay.wallet.api.v1.dto;

import com.vycepay.wallet.domain.model.AccountStatementJob;

import java.time.Instant;

public class StatementJobResponse {

    private String choiceRequestId;
    private String accountId;
    private String status;
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
