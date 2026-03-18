package com.vycepay.transaction.application.facade;

import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.choicebank.RequestIdGenerator;
import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.transaction.domain.model.Transaction;
import com.vycepay.transaction.domain.model.Wallet;
import com.vycepay.transaction.infrastructure.persistence.TransactionRepository;
import com.vycepay.transaction.infrastructure.persistence.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates transfers and deposits. Idempotent via idempotencyKey.
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class TransactionFacade {

    private static final Logger log = LoggerFactory.getLogger(TransactionFacade.class);
    private static final String STATUS_PENDING = "1";
    private static final String TYPE_TRANSFER = "TRANSFER";
    private static final String TYPE_DEPOSIT = "DEPOSIT";

    private final BankingProviderPort bankingProvider;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionFacade(BankingProviderPort bankingProvider,
                             TransactionRepository transactionRepository,
                             WalletRepository walletRepository) {
        this.bankingProvider = bankingProvider;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    /**
     * Initiates transfer. Idempotent - returns existing tx if key matches.
     *
     * @param customerId     Our customer ID
     * @param walletId       Wallet ID
     * @param choiceAccountId Choice account ID (payer)
     * @param payeeBankCode  Choice bank code (e.g. M-PESA)
     * @param payeeAccountId Recipient (M-PESA number or account)
     * @param amount         Amount
     * @param remark         Optional message to beneficiary (max 100 chars)
     * @param idempotencyKey Client-provided key
     * @return Transaction (existing or new)
     */
    @Transactional
    public Transaction applyTransfer(Long customerId, Long walletId, String choiceAccountId,
                                     String payeeBankCode, String payeeAccountId, String payeeAccountName,
                                     BigDecimal amount, String remark, String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> executeTransfer(customerId, walletId, choiceAccountId,
                        payeeBankCode, payeeAccountId, payeeAccountName, amount, remark, idempotencyKey));
    }

    private Transaction executeTransfer(Long customerId, Long walletId, String choiceAccountId,
                                        String payeeBankCode, String payeeAccountId, String payeeAccountName,
                                        BigDecimal amount, String remark, String idempotencyKey) {
        var params = new java.util.HashMap<String, Object>();
        params.put("payerAccountId", choiceAccountId);
        params.put("payeeBankCode", payeeBankCode);
        params.put("payeeAccountId", payeeAccountId);
        params.put("currency", "KES");
        params.put("amount", amount);
        if (payeeAccountName != null) params.put("payeeAccountName", payeeAccountName);
        if (remark != null && !remark.isBlank()) {
            params.put("remark", remark.length() > 100 ? remark.substring(0, 100) : remark);
        }
        ChoiceBankResponse response = bankingProvider.post("trans/v2/applyForTransfer", Map.copyOf(params));
        String txId = extractTxId(response.getData());
        String requestId = response.getRequestId() != null ? response.getRequestId() : RequestIdGenerator.generate();

        Transaction tx = new Transaction();
        tx.setExternalId(UUID.randomUUID().toString());
        tx.setCustomerId(customerId);
        tx.setWalletId(walletId);
        tx.setChoiceTxId(txId);
        tx.setChoiceRequestId(requestId);
        tx.setType(TYPE_TRANSFER);
        tx.setAmount(amount);
        tx.setCurrency("KES");
        tx.setStatus(STATUS_PENDING);
        tx.setPayeeBankCode(payeeBankCode);
        tx.setPayeeAccountId(payeeAccountId);
        tx.setPayeeAccountName(payeeAccountName);
        tx.setRemark(remark);
        tx.setIdempotencyKey(idempotencyKey);
        return transactionRepository.save(tx);
    }

    /**
     * Initiates M-PESA STK deposit. If idempotencyKey is provided and a transaction already exists with that key, returns it without calling Choice Bank.
     */
    @Transactional
    public Transaction depositFromMpesa(Long customerId, Long walletId, String choiceAccountId,
                                        String mobile, int amountKes, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseGet(() -> executeDepositFromMpesa(customerId, walletId, choiceAccountId, mobile, amountKes, idempotencyKey));
        }
        String internalKey = "deposit-" + choiceAccountId + "-" + System.currentTimeMillis();
        return executeDepositFromMpesa(customerId, walletId, choiceAccountId, mobile, amountKes, internalKey);
    }

    private Transaction executeDepositFromMpesa(Long customerId, Long walletId, String choiceAccountId,
                                               String mobile, int amountKes, String idempotencyKey) {
        var params = Map.<String, Object>of(
                "accountId", choiceAccountId,
                "mobile", mobile,
                "amount", amountKes);
        ChoiceBankResponse response = bankingProvider.post("trans/depositFromMpesa", params);
        String txId = extractTxId(response.getData());
        String requestId = response.getRequestId() != null ? response.getRequestId() : RequestIdGenerator.generate();

        Transaction tx = new Transaction();
        tx.setExternalId(UUID.randomUUID().toString());
        tx.setCustomerId(customerId);
        tx.setWalletId(walletId);
        tx.setChoiceTxId(txId);
        tx.setChoiceRequestId(requestId);
        tx.setType(TYPE_DEPOSIT);
        tx.setAmount(BigDecimal.valueOf(amountKes));
        tx.setCurrency("KES");
        tx.setStatus(STATUS_PENDING);
        tx.setPayeeAccountId(mobile);
        tx.setIdempotencyKey(idempotencyKey);
        return transactionRepository.save(tx);
    }

    /**
     * Sends OTP via Choice Bank for a pending transfer.
     * Used when Choice Bank requires OTP to complete the transaction.
     *
     * @param customerId         Our customer ID
     * @param transactionExternalId Our transaction external ID (UUID)
     * @param otpType            SMS or EMAIL
     */
    public void sendTransferOtp(Long customerId, String transactionExternalId, String otpType) {
        Transaction tx = transactionRepository.findByExternalIdAndCustomerId(transactionExternalId, customerId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", HttpStatus.NOT_FOUND));
        if (tx.getChoiceTxId() == null) {
            throw new BusinessException("INVALID_TRANSACTION", "Transaction has no Choice Bank reference", HttpStatus.CONFLICT);
        }
        var params = Map.<String, Object>of("businessId", tx.getChoiceTxId(), "otpType", otpType);
        ChoiceBankResponse response = bankingProvider.post("common/sendOtp", params);
        if (!response.isSuccess()) {
            throw new BusinessException("SEND_OTP_FAILED", "Send OTP failed: " + response.getMsg(), HttpStatus.BAD_GATEWAY);
        }
        log.info("OTP sent for transfer: txId={}", tx.getChoiceTxId());
    }

    /**
     * Resends OTP via Choice Bank (common/resendOtp) for a pending transfer.
     */
    public void resendTransferOtp(Long customerId, String transactionExternalId, String otpType) {
        Transaction tx = transactionRepository.findByExternalIdAndCustomerId(transactionExternalId, customerId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", HttpStatus.NOT_FOUND));
        if (tx.getChoiceTxId() == null) {
            throw new BusinessException("INVALID_TRANSACTION", "Transaction has no Choice Bank reference", HttpStatus.CONFLICT);
        }
        var params = Map.<String, Object>of("businessId", tx.getChoiceTxId(), "otpType", otpType);
        ChoiceBankResponse response = bankingProvider.post("common/resendOtp", params);
        if (!response.isSuccess()) {
            throw new BusinessException("RESEND_OTP_FAILED", "Resend OTP failed: " + response.getMsg(), HttpStatus.BAD_GATEWAY);
        }
        log.info("OTP resent for transfer: txId={}", tx.getChoiceTxId());
    }

    /**
     * Confirms OTP via Choice Bank for a pending transfer.
     *
     * @param customerId         Our customer ID
     * @param transactionExternalId Our transaction external ID
     * @param otpCode            OTP from user
     * @return true if confirmation succeeded
     */
    public boolean confirmTransferOtp(Long customerId, String transactionExternalId, String otpCode) {
        Transaction tx = transactionRepository.findByExternalIdAndCustomerId(transactionExternalId, customerId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", HttpStatus.NOT_FOUND));
        if (tx.getChoiceTxId() == null) {
            throw new BusinessException("INVALID_TRANSACTION", "Transaction has no Choice Bank reference", HttpStatus.CONFLICT);
        }
        var params = Map.<String, Object>of("businessId", tx.getChoiceTxId(), "otpCode", otpCode);
        ChoiceBankResponse response = bankingProvider.post("common/confirmOperation", params);
        return response.isSuccess();
    }

    /**
     * Queries Choice Bank for transaction status and optionally updates local record.
     * Use when callback is delayed or to poll pending transactions.
     *
     * @param customerId         Our customer ID
     * @param transactionExternalId Our transaction external ID
     * @return Choice Bank status data (status, msg, etc.) or empty map if not found
     */
    public Map<String, Object> queryTransactionStatus(Long customerId, String transactionExternalId) {
        Transaction tx = transactionRepository.findByExternalIdAndCustomerId(transactionExternalId, customerId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", HttpStatus.NOT_FOUND));
        if (tx.getChoiceTxId() == null) {
            throw new BusinessException("INVALID_TRANSACTION", "Transaction has no Choice Bank reference", HttpStatus.CONFLICT);
        }
        var params = Map.<String, Object>of("txId", tx.getChoiceTxId());
        ChoiceBankResponse response = bankingProvider.post("query/getTransResult", params);
        if (!response.isSuccess()) {
            log.warn("getTransResult failed for txId={}: {}", tx.getChoiceTxId(), response.getMsg());
            return Map.of("error", response.getMsg());
        }
        return response.getData() != null ? (Map<String, Object>) response.getData() : Map.of();
    }

    /**
     * Fetches bank codes from Choice Bank (staticData/getBankCodes) for transfer UI.
     */
    public Map<String, Object> getBankCodes() {
        ChoiceBankResponse response = bankingProvider.post("staticData/getBankCodes", Map.of());
        if (!response.isSuccess()) {
            log.warn("getBankCodes failed: {}", response.getMsg());
            return Map.of();
        }
        return response.getData() != null ? (Map<String, Object>) response.getData() : Map.of();
    }

    /**
     * Queries Choice Bank transaction list (query/getTransList) for an account.
     *
     * @param choiceAccountId Choice Bank account ID
     * @param userId          User ID in our system (Choice userId)
     * @param startTime       UTC timestamp ms
     * @param endTime         UTC timestamp ms
     * @param pageNo          Page number (1-based)
     * @param pageSize        Page size
     */
    public Map<String, Object> getChoiceTransList(String choiceAccountId, String userId,
                                                  long startTime, long endTime, int pageNo, int pageSize) {
        var params = new java.util.HashMap<String, Object>();
        params.put("accountId", choiceAccountId);
        params.put("userId", userId);
        params.put("txType", java.util.List.of());
        params.put("txStatus", java.util.List.of(2, 8));  // Processing, Succeeded
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("pageNo", pageNo);
        params.put("pageSize", pageSize);
        params.put("orderByDesc", 1);
        ChoiceBankResponse response = bankingProvider.post("query/getTransList", params);
        if (!response.isSuccess()) {
            log.warn("getTransList failed: {}", response.getMsg());
            return Map.of();
        }
        return response.getData() != null ? (Map<String, Object>) response.getData() : Map.of();
    }

    @SuppressWarnings("unchecked")
    private String extractTxId(Object data) {
        if (data instanceof Map) {
            Object id = ((Map<String, Object>) data).get("txId");
            return id != null ? id.toString() : null;
        }
        return null;
    }
}
