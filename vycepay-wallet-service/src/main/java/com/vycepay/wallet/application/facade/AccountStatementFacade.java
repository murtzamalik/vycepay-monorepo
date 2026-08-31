package com.vycepay.wallet.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankResult;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Account statement apply/query against Choice Bank email-delivery APIs;
 * persists job rows for polling (no download URL — statement is emailed).
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class AccountStatementFacade {

    private static final String PATH_APPLY_BANK_ACCOUNT_STATEMENT = "statement/applyBankAccountStatement";
    private static final String PATH_QUERY_BANK_ACCOUNT_STATEMENT = "statement/queryBankAccountStatement";

    /** Choice max statement period: 180 days. */
    private static final long MAX_STATEMENT_PERIOD_MS = TimeUnit.DAYS.toMillis(180);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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
     * Requests an account statement emailed by Choice to {@code email}.
     * Persists Choice {@code jobId} for query polling.
     *
     * @param email Destination address (customer-supplied; may differ from profile email)
     * @throws BusinessException if email blank/invalid or period exceeds 180 days
     */
    @Transactional
    public ChoiceBankResult applyAccountStatement(WalletAccountContext ctx,
                                                     String email,
                                                     long statementStartTime,
                                                     long statementEndTime,
                                                     Integer fileType) {
        String normalizedEmail = requireValidEmail(email);
        validateStatementPeriod(statementStartTime, statementEndTime);

        var params = new HashMap<String, Object>();
        params.put("accountId", ctx.choiceAccountId());
        // Mobile/BFF contract is Unix ms; Choice statement APIs validate against "now" in Unix seconds.
        // Passing 13-digit ms makes endTime always appear in the future → Choice "invalid end time".
        params.put("startTime", toChoiceUnixSeconds(statementStartTime));
        params.put("endTime", toChoiceUnixSeconds(statementEndTime));
        params.put("email", normalizedEmail);
        // Choice docs: email statements are Excel-only for now; default to xlsx when omitted.
        params.put("fileType", toChoiceFileType(fileType));
        ChoiceBankResponse response = bankingProvider.post(PATH_APPLY_BANK_ACCOUNT_STATEMENT, params);
        ChoiceBankResult choiceResult = choiceAssessor.requireSuccessResult(response, PATH_APPLY_BANK_ACCOUNT_STATEMENT);
        String jobId = extractStatementJobId(response);
        if (jobId == null || jobId.isBlank()) {
            throw new BusinessException("CHOICE_BANK_ERROR", "Missing statement job id from Choice Bank",
                    HttpStatus.BAD_GATEWAY);
        }
        AccountStatementJob job = new AccountStatementJob();
        job.setCustomerId(ctx.customerId());
        job.setChoiceRequestId(jobId);
        job.setAccountId(ctx.choiceAccountId());
        job.setEmail(normalizedEmail);
        job.setStatus(AccountStatementJob.STATUS_PENDING);
        statementJobRepository.save(job);

        Map<String, Object> out = new HashMap<>();
        out.put("choiceRequestId", jobId);
        out.put("jobId", jobId);
        out.put("email", normalizedEmail);
        if (choiceResult.data() instanceof Map<?, ?> d) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dm = (Map<String, Object>) d;
            out.putAll(dm);
        }
        return new ChoiceBankResult(out, choiceResult.msg(), choiceResult.choiceRequestId());
    }

    /**
     * Queries email-statement job status from Choice Bank and merges local job state when present.
     */
    @Transactional
    public ChoiceBankResult queryAccountStatement(WalletAccountContext ctx, String requestId) {
        statementJobRepository.findByChoiceRequestIdAndCustomerId(requestId, ctx.customerId())
                .orElseThrow(() -> new BusinessException("STATEMENT_JOB_NOT_FOUND",
                        "Unknown statement request for this customer", HttpStatus.NOT_FOUND));

        var params = Map.<String, Object>of("jobId", requestId);
        ChoiceBankResponse response = bankingProvider.post(PATH_QUERY_BANK_ACCOUNT_STATEMENT, params);
        ChoiceBankResult choiceResult = choiceAssessor.requireSuccessResult(response, PATH_QUERY_BANK_ACCOUNT_STATEMENT);
        Map<String, Object> out = new HashMap<>();
        if (choiceResult.data() instanceof Map<?, ?> d) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dm = (Map<String, Object>) d;
            out.putAll(dm);
        }
        statementJobRepository.findByChoiceRequestId(requestId).ifPresent(job -> {
            mergeChoiceQueryIntoJob(job, out);
            out.put("localStatus", job.getStatus());
            if (job.getEmail() != null) {
                out.putIfAbsent("email", job.getEmail());
            }
        });
        return new ChoiceBankResult(out, choiceResult.msg(), choiceResult.choiceRequestId());
    }

    /**
     * Maps Choice {@code status} 0→PENDING, 1→READY. Email API does not return a download URL.
     */
    private void mergeChoiceQueryIntoJob(AccountStatementJob job, Map<String, Object> choiceData) {
        Object email = choiceData.get("email");
        if (email != null && !email.toString().isBlank() && job.getEmail() == null) {
            job.setEmail(email.toString().trim());
        }
        Object status = choiceData.get("status");
        if (status instanceof Number n) {
            if (n.intValue() == 1) {
                job.setStatus(AccountStatementJob.STATUS_READY);
                statementJobRepository.save(job);
            } else if (n.intValue() == 0) {
                job.setStatus(AccountStatementJob.STATUS_PENDING);
                statementJobRepository.save(job);
            }
            return;
        }
        if (status != null) {
            try {
                int s = Integer.parseInt(status.toString().trim());
                if (s == 1) {
                    job.setStatus(AccountStatementJob.STATUS_READY);
                    statementJobRepository.save(job);
                } else if (s == 0) {
                    job.setStatus(AccountStatementJob.STATUS_PENDING);
                    statementJobRepository.save(job);
                }
            } catch (NumberFormatException ignored) {
                // leave local status unchanged
            }
        }
    }

    /**
     * Local statement job status (updated by query polling; legacy callbacks 0009/0015 may still update old rows).
     */
    public AccountStatementJob getLocalStatementJob(WalletAccountContext ctx, String choiceRequestId) {
        return statementJobRepository.findByChoiceRequestIdAndCustomerId(choiceRequestId, ctx.customerId())
                .orElseThrow(() -> new BusinessException("STATEMENT_JOB_NOT_FOUND",
                        "Statement job not found", HttpStatus.NOT_FOUND));
    }

    private static String requireValidEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("EMAIL_REQUIRED", "email is required", HttpStatus.BAD_REQUEST);
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException("INVALID_EMAIL", "email format is invalid", HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private static void validateStatementPeriod(long startTime, long endTime) {
        if (endTime < startTime) {
            throw new BusinessException("INVALID_STATEMENT_PERIOD",
                    "statementEndTime must be greater than or equal to statementStartTime",
                    HttpStatus.BAD_REQUEST);
        }
        if (endTime - startTime > MAX_STATEMENT_PERIOD_MS) {
            throw new BusinessException("INVALID_STATEMENT_PERIOD",
                    "Statement period must not exceed 180 days",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Converts BFF/mobile Unix milliseconds to Choice statement Unix seconds.
     */
    private static long toChoiceUnixSeconds(long unixMs) {
        return unixMs / 1000L;
    }

    /**
     * Maps mobile/API fileType (0=PDF, 1=Excel) to Choice Bank string values.
     * Null defaults to {@code xlsx} (Choice email statements are Excel-only for now).
     */
    private static String toChoiceFileType(Integer fileType) {
        if (fileType == null) {
            return "xlsx";
        }
        return switch (fileType) {
            case 0 -> "pdf";
            case 1 -> "xlsx";
            default -> throw new BusinessException("INVALID_FILE_TYPE",
                    "fileType must be 0 (PDF) or 1 (Excel)", HttpStatus.BAD_REQUEST);
        };
    }

    /**
     * Choice applyBankAccountStatement returns {@code jobId} in data; falls back to envelope requestId.
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
