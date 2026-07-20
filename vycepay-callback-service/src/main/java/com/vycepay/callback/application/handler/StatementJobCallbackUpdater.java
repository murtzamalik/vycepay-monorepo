package com.vycepay.callback.application.handler;

import com.vycepay.callback.domain.model.AccountStatementJob;
import com.vycepay.callback.infrastructure.persistence.AccountStatementJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Updates local statement jobs from Choice Bank statement callbacks (0009 legacy, 0015 file job).
 */
@Component
public class StatementJobCallbackUpdater {

    private static final Logger log = LoggerFactory.getLogger(StatementJobCallbackUpdater.class);

    private final AccountStatementJobRepository statementJobRepository;

    public StatementJobCallbackUpdater(AccountStatementJobRepository statementJobRepository) {
        this.statementJobRepository = statementJobRepository;
    }

    /**
     * Correlates callback params to a persisted job and merges download URL / status.
     *
     * @return updated job when found, empty otherwise
     */
    public Optional<AccountStatementJob> updateFromParams(Map<String, Object> params, String fallbackCorrelationId) {
        String jobId = firstNonBlank(
                getString(params, "jobId"),
                getString(params, "requestId"),
                getString(params, "statementRequestId"),
                fallbackCorrelationId);
        if (jobId == null || jobId.isBlank()) {
            log.warn("Statement callback missing job id");
            return Optional.empty();
        }
        return statementJobRepository.findByChoiceRequestId(jobId).map(job -> {
            applyParams(job, params);
            return job;
        }).or(() -> {
            log.warn("No local statement job for jobId={}", jobId);
            return Optional.empty();
        });
    }

    private void applyParams(AccountStatementJob job, Map<String, Object> params) {
        String fileUrl = firstNonBlank(
                getString(params, "fileUrl"),
                getString(params, "statementUrl"),
                getString(params, "downloadUrl"),
                getString(params, "url"));
        String fileName = getString(params, "fileName");
        Integer st = getInt(params, "status");
        String err = getString(params, "errorMsg");
        if (fileUrl != null && !fileUrl.isBlank()) {
            job.setDownloadUrl(fileUrl);
            job.setStatus(AccountStatementJob.STATUS_READY);
        } else if (err != null && !err.isBlank()) {
            job.setErrorMsg(err);
            job.setStatus(AccountStatementJob.STATUS_FAILED);
        } else if (st != null && st == 1) {
            job.setStatus(AccountStatementJob.STATUS_READY);
        }
        if (fileName != null) {
            job.setFileName(fileName);
        }
        statementJobRepository.save(job);
        log.info("Updated account statement job jobId={} status={}", job.getChoiceRequestId(), job.getStatus());
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String s : values) {
            if (s != null && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    private static String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private static Integer getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
