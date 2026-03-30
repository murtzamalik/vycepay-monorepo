package com.vycepay.wallet.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.application.WalletAccountContext;
import com.vycepay.wallet.domain.model.AccountStatementJob;
import com.vycepay.wallet.infrastructure.persistence.AccountStatementJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AccountStatementFacade.class);

    private final BankingProviderPort bankingProvider;
    private final AccountStatementJobRepository statementJobRepository;

    public AccountStatementFacade(BankingProviderPort bankingProvider,
                                  AccountStatementJobRepository statementJobRepository) {
        this.bankingProvider = bankingProvider;
        this.statementJobRepository = statementJobRepository;
    }

    /**
     * Applies for a periodic account statement. Persists choice_request_id for webhook 0009 correlation.
     */
    @Transactional
    public Map<String, Object> applyAccountStatement(WalletAccountContext ctx,
                                                     long statementStartTime,
                                                     long statementEndTime,
                                                     Integer fileType) {
        var params = new HashMap<String, Object>();
        params.put("accountId", ctx.choiceAccountId());
        params.put("statementStartTime", statementStartTime);
        params.put("statementEndTime", statementEndTime);
        if (fileType != null) {
            params.put("fileType", fileType);
        }
        ChoiceBankResponse response = bankingProvider.post("statement/applyAccountStatement", params);
        if (!response.isSuccess()) {
            log.warn("applyAccountStatement failed: {}", response.getMsg());
            throw new BusinessException("CHOICE_BANK_ERROR",
                    response.getMsg() != null ? response.getMsg() : "Statement apply failed",
                    HttpStatus.BAD_GATEWAY);
        }
        String choiceRequestId = response.getRequestId();
        if (choiceRequestId == null || choiceRequestId.isBlank()) {
            throw new BusinessException("CHOICE_BANK_ERROR", "Missing request id from Choice Bank",
                    HttpStatus.BAD_GATEWAY);
        }
        AccountStatementJob job = new AccountStatementJob();
        job.setCustomerId(ctx.customerId());
        job.setChoiceRequestId(choiceRequestId);
        job.setAccountId(ctx.choiceAccountId());
        job.setStatus(AccountStatementJob.STATUS_PENDING);
        statementJobRepository.save(job);

        Map<String, Object> out = new HashMap<>();
        out.put("choiceRequestId", choiceRequestId);
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

        var params = Map.<String, Object>of("requestId", requestId);
        ChoiceBankResponse response = bankingProvider.post("statement/queryAccountStatement", params);
        if (!response.isSuccess()) {
            throw new BusinessException("CHOICE_BANK_ERROR",
                    response.getMsg() != null ? response.getMsg() : "Statement query failed",
                    HttpStatus.BAD_GATEWAY);
        }
        Map<String, Object> out = new HashMap<>();
        if (response.getData() instanceof Map<?, ?> d) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dm = (Map<String, Object>) d;
            out.putAll(dm);
        }
        statementJobRepository.findByChoiceRequestId(requestId).ifPresent(job -> {
            out.put("localStatus", job.getStatus());
            if (job.getDownloadUrl() != null) {
                out.put("localDownloadUrl", job.getDownloadUrl());
            }
        });
        return out;
    }

    /**
     * Local statement job status (updated by callback 0009 or polling).
     */
    public AccountStatementJob getLocalStatementJob(WalletAccountContext ctx, String choiceRequestId) {
        return statementJobRepository.findByChoiceRequestIdAndCustomerId(choiceRequestId, ctx.customerId())
                .orElseThrow(() -> new BusinessException("STATEMENT_JOB_NOT_FOUND",
                        "Statement job not found", HttpStatus.NOT_FOUND));
    }
}
