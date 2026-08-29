package com.vycepay.callback.application.service;

import com.vycepay.callback.domain.model.Transaction;
import com.vycepay.callback.domain.model.Wallet;
import com.vycepay.callback.infrastructure.activity.CustomerActivityRecorder;
import com.vycepay.callback.infrastructure.persistence.TransactionRepository;
import com.vycepay.callback.infrastructure.persistence.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Upserts a local {@code transaction} row for inbound Choice money events that VycePay
 * did not initiate (e.g. unsolicited Pay Bill credits notified via 0003 / unmatched 0002).
 * App-originated rows (deposit/send) are never overwritten for type/amount.
 */
@Service
public class InboundMoneyEventService {

    private static final Logger log = LoggerFactory.getLogger(InboundMoneyEventService.class);
    private static final String ACTIVITY_INBOUND = "INBOUND_CREDIT_RECORDED";

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final CustomerActivityRecorder activityRecorder;

    public InboundMoneyEventService(TransactionRepository transactionRepository,
                                    WalletRepository walletRepository,
                                    CustomerActivityRecorder activityRecorder) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.activityRecorder = activityRecorder;
    }

    /**
     * Finds or creates a local transaction for the Choice {@code txId}.
     *
     * @param choiceTxId       Choice transaction id (required)
     * @param accountId        Choice account id (wallet lookup when inserting)
     * @param choiceRequestId  Envelope requestId (required for insert; NOT NULL column)
     * @param params           Callback params map
     * @param statusOverride   Status for insert (e.g. "8" for 0003); null uses txStatus from params
     * @return existing or newly inserted transaction, or empty when cannot resolve/insert
     */
    @Transactional
    public Optional<Transaction> upsertInboundCredit(String choiceTxId,
                                                     String accountId,
                                                     String choiceRequestId,
                                                     Map<String, Object> params,
                                                     String statusOverride) {
        if (choiceTxId == null || choiceTxId.isBlank()) {
            return Optional.empty();
        }
        Optional<Transaction> existing = transactionRepository.findByChoiceTxId(choiceTxId);
        if (existing.isPresent()) {
            return existing;
        }

        String amountRaw = getString(params, "amount");
        if (isOutbound(amountRaw)) {
            log.info("Skip inbound upsert for debit choiceTxId={} amount={}", choiceTxId, amountRaw);
            return Optional.empty();
        }

        Optional<Wallet> walletOpt = accountId != null && !accountId.isBlank()
                ? walletRepository.findByChoiceAccountId(accountId)
                : Optional.empty();
        if (walletOpt.isEmpty()) {
            log.warn("Cannot upsert inbound credit: wallet not found for accountId={} txId={}",
                    accountId, choiceTxId);
            return Optional.empty();
        }
        Wallet wallet = walletOpt.get();

        String requestId = firstNonBlank(choiceRequestId, getString(params, "requestId"), choiceTxId);
        if (requestId == null || requestId.isBlank()) {
            log.warn("Cannot upsert inbound credit: missing choiceRequestId for txId={}", choiceTxId);
            return Optional.empty();
        }

        try {
            Transaction tx = new Transaction();
            tx.setExternalId(UUID.randomUUID().toString());
            tx.setCustomerId(wallet.getCustomerId());
            tx.setWalletId(wallet.getId());
            tx.setChoiceTxId(choiceTxId);
            tx.setChoiceRequestId(requestId);
            tx.setType(Transaction.TYPE_DEPOSIT);
            tx.setAmount(parseAbsoluteAmount(amountRaw));
            tx.setCurrency(firstNonBlank(getString(params, "currency"), "KES"));
            tx.setFeeAmount(BigDecimal.ZERO);
            String status = firstNonBlank(statusOverride, getString(params, "txStatus"), Transaction.STATUS_SUCCESS);
            tx.setStatus(status);
            tx.setPayeeBankCode(getString(params, "oppoBankCode"));
            tx.setPayeeAccountId(getString(params, "oppoAccountId"));
            tx.setPayeeAccountName(firstNonBlank(
                    getString(params, "oppoAccountName"),
                    nestedString(params, "extInfo", "counterpartyName")));
            tx.setIdempotencyKey("inbound-" + choiceTxId);
            Long completeMs = getLong(params, "completeTime");
            if (completeMs == null) {
                completeMs = getLong(params, "updateTime");
            }
            tx.setCompletedAt(completeMs != null ? Instant.ofEpochMilli(completeMs) : Instant.now());

            Transaction saved = transactionRepository.save(tx);
            activityRecorder.record(wallet.getCustomerId(), ACTIVITY_INBOUND, "TRANSACTION", saved.getExternalId());
            log.info("Inserted inbound DEPOSIT choiceTxId={} externalId={} customerId={}",
                    choiceTxId, saved.getExternalId(), wallet.getCustomerId());
            return Optional.of(saved);
        } catch (DataIntegrityViolationException e) {
            log.debug("Inbound upsert race for choiceTxId={}: {}", choiceTxId, e.getMessage());
            return transactionRepository.findByChoiceTxId(choiceTxId);
        }
    }

    private static boolean isOutbound(String amount) {
        if (amount == null || amount.isBlank()) {
            return false;
        }
        try {
            return new BigDecimal(amount.trim()).compareTo(BigDecimal.ZERO) < 0;
        } catch (NumberFormatException e) {
            return amount.trim().startsWith("-");
        }
    }

    private static BigDecimal parseAbsoluteAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(amount.trim()).abs();
        } catch (NumberFormatException e) {
            String normalized = amount.trim().startsWith("-") ? amount.trim().substring(1) : amount.trim();
            try {
                return new BigDecimal(normalized);
            } catch (NumberFormatException e2) {
                return BigDecimal.ZERO;
            }
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String s : values) {
            if (s != null && !s.isBlank() && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        return null;
    }

    private static String getString(Map<String, Object> params, String key) {
        if (params == null) {
            return null;
        }
        Object v = params.get(key);
        if (v == null) {
            return null;
        }
        String s = v.toString();
        return "null".equalsIgnoreCase(s) ? null : s;
    }

    @SuppressWarnings("unchecked")
    private static String nestedString(Map<String, Object> params, String objectKey, String field) {
        if (params == null) {
            return null;
        }
        Object nested = params.get(objectKey);
        if (!(nested instanceof Map<?, ?> map)) {
            return null;
        }
        Object v = map.get(field);
        return v != null ? v.toString() : null;
    }

    private static Long getLong(Map<String, Object> params, String key) {
        if (params == null) {
            return null;
        }
        Object v = params.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
