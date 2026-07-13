package com.vycepay.wallet.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.application.WalletAccountContext;
import com.vycepay.wallet.domain.model.AccountStatementJob;
import com.vycepay.wallet.infrastructure.persistence.AccountStatementJobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Periodic account statement apply/query against Choice Bank; persists job rows for callbacks and polling.
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class AccountStatementFacade {

    private static final String PATH_APPLY_ACCOUNT_STATEMENT = "statement/applyAccountStatement";
    private static final String PATH_QUERY_ACCOUNT_STATEMENT = "statement/queryAccountStatement";

    private final BankingProviderPort bankingProvider;
    private final ChoiceBankResponseAssessor choiceAssessor;
    private final AccountStatementJobRepository statementJobRepository;

    public AccountStatementFacade(BankingProviderPort bankingProvider,
                                  ChoiceBankResponseAssessor choiceAssessor,
                                  AccountStatementJobRepository statementJobRepository) {
        this.bankingProvider = bankingProvider;
        this.choiceAssessor = choiceAssessor;
        this.statementJobRepository = statementJobRepository;
    }

    /**
     * Applies for a periodic account statement. Persists Choice {@code jobId} for query and webhook correlation.
     */
    @Transactional
    public Map<String, Object> applyAccountStatement(WalletAccountContext ctx,
                                                     long statementStartTime,
                                                     long statementEndTime,
                                                     Integer fileType) {
        var params = new HashMap<String, Object>();
        params.put("accountId", ctx.choiceAccountId());
        // Choice Bank: startTime/endTime (Unix ms), fileType as "pdf" or "xlsx".
        params.put("startTime", statementStartTime);
        params.put("endTime", statementEndTime);
        String choiceFileType = toChoiceFileType(fileType);
        if (choiceFileType != null) {
            params.put("fileType", choiceFileType);
        }
        ChoiceBankResponse response = bankingProvider.post(PATH_APPLY_ACCOUNT_STATEMENT, params);
        choiceAssessor.requireSuccess(response, PATH_APPLY_ACCOUNT_STATEMENT);
        String jobId = extractStatementJobId(response);
        if (jobId == null || jobId.isBlank()) {
            throw new BusinessException("CHOICE_BANK_ERROR", "Missing statement job id from Choice Bank",
                    HttpStatus.BAD_GATEWAY);
        }
        AccountStatementJob job = new AccountStatementJob();
        job.setCustomerId(ctx.customerId());
        job.setChoiceRequestId(jobId);
        job.setAccountId(ctx.choiceAccountId());
        job.setStatus(AccountStatementJob.STATUS_PENDING);
        statementJobRepository.save(job);

        Map<String, Object> out = new HashMap<>();
        out.put("choiceRequestId", jobId);
        out.put("jobId", jobId);
        if (response.getData() instanceof Map<?, ?> d) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dm = (Map<String, Object>) d;
            out.putAll(dm);
        }
        return out;
    }

    /**
     * Queries statement generation status from Choice Bank and merges local job state when present.
     */
    public Map<String, Object> queryAccountStatement(WalletAccountContext ctx, String requestId) {
        statementJobRepository.findByChoiceRequestIdAndCustomerId(requestId, ctx.customerId())
                .orElseThrow(() -> new BusinessException("STATEMENT_JOB_NOT_FOUND",
                        "Unknown statement request for this customer", HttpStatus.NOT_FOUND));

        var params = Map.<String, Object>of("jobId", requestId);
        ChoiceBankResponse response = bankingProvider.post(PATH_QUERY_ACCOUNT_STATEMENT, params);
        choiceAssessor.requireSuccess(response, PATH_QUERY_ACCOUNT_STATEMENT);
        Map<String, Object> out = new HashMap<>();
        if (response.getData() instanceof Map<?, ?> d) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dm = (Map<String, Object>) d;
            out.putAll(dm);
        }
        statementJobRepository.findByChoiceRequestId(requestId).ifPresent(job -> {
            mergeChoiceQueryIntoJob(job, out);
            out.put("localStatus", job.getStatus());
            if (job.getDownloadUrl() != null) {
                out.put("localDownloadUrl", job.getDownloadUrl());
            }
        });
        return out;
    }

    private void mergeChoiceQueryIntoJob(AccountStatementJob job, Map<String, Object> choiceData) {
        Object url = firstNonNull(choiceData.get("statementUrl"), choiceData.get("fileUrl"),
                choiceData.get("downloadUrl"));
        if (url != null && !url.toString().isBlank()) {
            job.setDownloadUrl(url.toString());
            job.setStatus(AccountStatementJob.STATUS_READY);
            statementJobRepository.save(job);
            return;
        }
        Object status = choiceData.get("status");
        if (status instanceof Number n && n.intValue() == 1) {
            job.setStatus(AccountStatementJob.STATUS_READY);
            statementJobRepository.save(job);
        }
    }

    private static Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * Local statement job status (updated by callback 0009 or polling).
     */
    public AccountStatementJob getLocalStatementJob(WalletAccountContext ctx, String choiceRequestId) {
        return statementJobRepository.findByChoiceRequestIdAndCustomerId(choiceRequestId, ctx.customerId())
                .orElseThrow(() -> new BusinessException("STATEMENT_JOB_NOT_FOUND",
                        "Statement job not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Maps mobile/API fileType (0=PDF, 1=Excel) to Choice Bank string values.
     */
    private static String toChoiceFileType(Integer fileType) {
        if (fileType == null) {
            return null;
        }
        return switch (fileType) {
            case 0 -> "pdf";
            case 1 -> "xlsx";
            default -> throw new BusinessException("INVALID_FILE_TYPE",
                    "fileType must be 0 (PDF) or 1 (Excel)", HttpStatus.BAD_REQUEST);
        };
    }

    /**
     * Choice applyAccountStatement returns {@code jobId} in data; falls back to envelope requestId.
     */
    private static String extractStatementJobId(ChoiceBankResponse response) {
        if (response.getData() instanceof Map<?, ?> data) {
            Object jobId = data.get("jobId");
            if (jobId != null && !jobId.toString().isBlank()) {
                return jobId.toString();
            }
        }
        return response.getRequestId();
    }
}
