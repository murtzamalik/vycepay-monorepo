package com.vycepay.transaction.application.facade;

import com.vycepay.common.choicebank.RequestIdGenerator;
import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.transaction.domain.model.Transaction;
import com.vycepay.transaction.infrastructure.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Choice Bank utility payments (airtime, bill query/pay, payment queries). Debits create local transactions for callback 0002.
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class UtilityPaymentFacade {

    private static final Logger log = LoggerFactory.getLogger(UtilityPaymentFacade.class);
    private static final String STATUS_PENDING = "1";
    private static final String TYPE_UTILITY_AIRTIME = "UTILITY_AIRTIME";
    private static final String TYPE_UTILITY_AIRTIME_BULK = "UTILITY_AIRTIME_BULK";
    private static final String TYPE_UTILITY_BILL_PAYMENT = "UTILITY_BILL_PAYMENT";

    private final BankingProviderPort bankingProvider;
    private final TransactionRepository transactionRepository;

    public UtilityPaymentFacade(BankingProviderPort bankingProvider,
                                TransactionRepository transactionRepository) {
        this.bankingProvider = bankingProvider;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction airtimePayment(Long customerId, Long walletId, String choiceAccountId,
                                    Map<String, Object> params, String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> executeDebit("utilityPayment/v2/airtimePayment", TYPE_UTILITY_AIRTIME,
                        customerId, walletId, choiceAccountId, params, idempotencyKey));
    }

    @Transactional
    public Transaction airtimeBulkPayment(Long customerId, Long walletId, String choiceAccountId,
                                          Map<String, Object> params, String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> executeDebit("utilityPayment/v2/airtimeBulkPayment", TYPE_UTILITY_AIRTIME_BULK,
                        customerId, walletId, choiceAccountId, params, idempotencyKey));
    }

    @Transactional
    public Transaction billPayment(Long customerId, Long walletId, String choiceAccountId,
                                   Map<String, Object> params, String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> executeDebit("utilityPayment/v2/billPayment", TYPE_UTILITY_BILL_PAYMENT,
                        customerId, walletId, choiceAccountId, params, idempotencyKey));
    }

    public Map<String, Object> billQuery(String choiceAccountId, Map<String, Object> params) {
        var p = new HashMap<String, Object>();
        if (params != null) {
            p.putAll(params);
        }
        p.putIfAbsent("accountId", choiceAccountId);
        ChoiceBankResponse response = bankingProvider.post("utilityPayment/billQuery", p);
        return unwrap(response);
    }

    public Map<String, Object> paymentQuery(String choiceAccountId, Map<String, Object> params) {
        var p = new HashMap<String, Object>();
        if (params != null) {
            p.putAll(params);
        }
        p.putIfAbsent("accountId", choiceAccountId);
        ChoiceBankResponse response = bankingProvider.post("utilityPayment/paymentQuery", p);
        return unwrap(response);
    }

    public Map<String, Object> bulkPaymentQuery(String choiceAccountId, Map<String, Object> params) {
        var p = new HashMap<String, Object>();
        if (params != null) {
            p.putAll(params);
        }
        p.putIfAbsent("accountId", choiceAccountId);
        ChoiceBankResponse response = bankingProvider.post("utilityPayment/bulkPaymentQuery", p);
        return unwrap(response);
    }

    private Transaction executeDebit(String path, String type,
                                     Long customerId, Long walletId, String choiceAccountId,
                                     Map<String, Object> requestParams, String idempotencyKey) {
        var params = new HashMap<String, Object>();
        if (requestParams != null) {
            params.putAll(requestParams);
        }
        params.put("accountId", choiceAccountId);
        ChoiceBankResponse response = bankingProvider.post(path, params);
        if (!response.isSuccess()) {
            throw new BusinessException("UTILITY_PAYMENT_FAILED",
                    response.getMsg() != null ? response.getMsg() : "Utility payment failed",
                    HttpStatus.BAD_GATEWAY);
        }
        String txId = extractTxId(response.getData());
        String requestId = response.getRequestId() != null ? response.getRequestId() : RequestIdGenerator.generate();
        BigDecimal amount = extractAmount(params);

        Transaction tx = new Transaction();
        tx.setExternalId(UUID.randomUUID().toString());
        tx.setCustomerId(customerId);
        tx.setWalletId(walletId);
        tx.setChoiceTxId(txId);
        tx.setChoiceRequestId(requestId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCurrency("KES");
        tx.setStatus(STATUS_PENDING);
        tx.setRemark(remarkFrom(params));
        tx.setIdempotencyKey(idempotencyKey);
        return transactionRepository.save(tx);
    }

    private static String remarkFrom(Map<String, Object> params) {
        Object r = params.get("remark");
        return r != null ? r.toString() : "UTILITY";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrap(ChoiceBankResponse response) {
        if (!response.isSuccess()) {
            log.warn("Utility query failed: {}", response.getMsg());
            throw new BusinessException("UTILITY_QUERY_FAILED",
                    response.getMsg() != null ? response.getMsg() : "Utility query failed",
                    HttpStatus.BAD_GATEWAY);
        }
        if (response.getData() instanceof Map) {
            return (Map<String, Object>) response.getData();
        }
        return Map.of();
    }

    public static BigDecimal extractAmount(Map<String, Object> params) {
        Object a = params.get("amount");
        if (a == null) {
            return BigDecimal.ZERO;
        }
        if (a instanceof BigDecimal) {
            return (BigDecimal) a;
        }
        if (a instanceof Number) {
            return BigDecimal.valueOf(((Number) a).doubleValue());
        }
        try {
            return new BigDecimal(a.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractTxId(Object data) {
        if (data instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) data;
            Object id = m.get("txId");
            if (id == null) {
                id = m.get("batchId");
            }
            if (id == null) {
                id = m.get("utilityTxId");
            }
            return id != null ? id.toString() : null;
        }
        return null;
    }
}
