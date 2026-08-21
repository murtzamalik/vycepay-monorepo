package com.vycepay.transaction.api.v1;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vycepay.transaction.api.v1.dto.SendMoneyRequest;
import com.vycepay.transaction.api.v1.dto.TransactionResponse;
import com.vycepay.transaction.api.v1.dto.ValidateAccountRequest;
import com.vycepay.transaction.api.v1.dto.ValidateAccountResponse;
import com.vycepay.transaction.application.TransactionChoiceOutcome;
import com.vycepay.transaction.application.facade.TransactionFacade;
import io.swagger.v3.oas.annotations.Operation;
import com.vycepay.transaction.domain.model.Transaction;
import com.vycepay.transaction.domain.model.TransactionDisplayStatus;
import com.vycepay.transaction.infrastructure.persistence.CustomerRepository;
import com.vycepay.transaction.infrastructure.persistence.KycVerificationRepository;
import com.vycepay.transaction.infrastructure.persistence.TransactionRepository;
import com.vycepay.transaction.infrastructure.persistence.TransactionSpecification;
import com.vycepay.transaction.infrastructure.persistence.WalletRepository;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.choicebank.errors.ChoiceBankResult;
import com.vycepay.common.choicebank.errors.ChoiceCustomerMessage;
import com.vycepay.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.http.HttpStatus;

import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Transaction API: send money, deposit, history.
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionFacade transactionFacade;
    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final KycVerificationRepository kycVerificationRepository;

    public TransactionController(
            @org.springframework.beans.factory.annotation.Autowired(required = false) TransactionFacade transactionFacade,
            CustomerRepository customerRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            KycVerificationRepository kycVerificationRepository) {
        this.transactionFacade = transactionFacade;
        this.customerRepository = customerRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.kycVerificationRepository = kycVerificationRepository;
    }

    /**
     * Validates recipient account via Choice Hakikisha (title fetch) before transfer.
     * Does not require a wallet; customer must be authenticated.
     */
    @Operation(summary = "Validate account / fetch title (Hakikisha)")
    @PostMapping("/validate-account")
    public ResponseEntity<ValidateAccountResponse> validateAccount(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody ValidateAccountRequest request) {
        if (transactionFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Transaction service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        var result = transactionFacade.validateAccount(
                request.getAccountId(), request.getAccountType(), request.getBankCode());
        return ResponseEntity.ok(result);
    }

    /**
     * Initiates transfer. Idempotency-Key header required.
     * Re-validates payee via Hakikisha and stores Choice-returned accountName.
     */
    @PostMapping("/send")
    public ResponseEntity<TransactionResponse> send(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody SendMoneyRequest request) {
        if (transactionFacade == null) return ResponseEntity.status(503).build();
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        var wallet = walletRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Wallet not found", HttpStatus.NOT_FOUND));
        var outcome = transactionFacade.applyTransfer(
                customer.getId(), wallet.getId(), wallet.getChoiceAccountId(),
                request.getPayeeBankCode(), request.getPayeeAccountId(), request.getAccountType(),
                request.getAmount(), request.getRemark(), idempotencyKey);
        return ResponseEntity.ok(toResponse(outcome));
    }

    /**
     * M-PESA STK Push deposit. Optional Idempotency-Key: when provided, duplicate requests return the same transaction.
     */
    @PostMapping("/deposit/mpesa")
    public ResponseEntity<TransactionResponse> depositMpesa(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam String mobile,
            @RequestParam int amount) {
        if (transactionFacade == null) return ResponseEntity.status(503).build();
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        var wallet = walletRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Wallet not found", HttpStatus.NOT_FOUND));
        var outcome = transactionFacade.depositFromMpesa(
                customer.getId(), wallet.getId(), wallet.getChoiceAccountId(), mobile, amount, idempotencyKey);
        return ResponseEntity.ok(toResponse(outcome));
    }

    /**
     * Sends OTP for a pending transfer (Choice Bank common/sendOtp).
     *
     * @param transactionId The externalId (UUID) from POST /transactions/send response
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiSuccessResponse<Void>> sendOtp(
            @RequestHeader("X-Customer-Id") String externalId,
            @Parameter(description = "Transaction externalId (UUID) from POST /transactions/send response")
            @RequestParam(name = "transactionId") String transactionId,
            @RequestParam(defaultValue = "SMS") String otpType) {
        if (transactionFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Transaction service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        String choiceMsg = transactionFacade.sendTransferOtp(customer.getId(), transactionId, otpType);
        return ResponseEntity.ok(ApiSuccessResponses.ok("TXN_OTP_SENT",
                ChoiceCustomerMessage.prefer(choiceMsg, "Transaction OTP sent successfully.")));
    }

    /**
     * Resends OTP for a pending transfer (Choice Bank common/resendOtp).
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiSuccessResponse<Void>> resendOtp(
            @RequestHeader("X-Customer-Id") String externalId,
            @Parameter(description = "Transaction externalId (UUID) from POST /transactions/send response")
            @RequestParam String transactionId,
            @RequestParam(defaultValue = "SMS") String otpType) {
        if (transactionFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Transaction service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        String choiceMsg = transactionFacade.resendTransferOtp(customer.getId(), transactionId, otpType);
        return ResponseEntity.ok(ApiSuccessResponses.ok("TXN_OTP_RESENT",
                ChoiceCustomerMessage.prefer(choiceMsg, "Transaction OTP resent successfully.")));
    }

    /**
     * Confirms OTP for a pending transfer (Choice Bank common/confirmOperation).
     */
    @PostMapping("/confirm-otp")
    public ResponseEntity<ApiSuccessResponse<Void>> confirmOtp(
            @RequestHeader("X-Customer-Id") String externalId,
            @Parameter(description = "Transaction externalId (UUID) from POST /transactions/send response")
            @RequestParam String transactionId,
            @RequestParam String otpCode) {
        if (transactionFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Transaction service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        String choiceMsg = transactionFacade.confirmTransferOtp(customer.getId(), transactionId, otpCode);
        return ResponseEntity.ok(ApiSuccessResponses.ok("TXN_OTP_CONFIRMED",
                ChoiceCustomerMessage.prefer(choiceMsg, "Transaction OTP confirmed successfully.")));
    }

    /**
     * Returns full local transaction detail by externalId.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getDetail(
            @RequestHeader("X-Customer-Id") String externalId,
            @Parameter(description = "Transaction externalId (UUID) from POST /transactions/send response")
            @PathVariable String transactionId) {
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        var tx = transactionRepository.findByExternalIdAndCustomerId(transactionId, customer.getId())
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(toResponse(tx));
    }

    /**
     * Queries Choice Bank for transaction status (getTransResult). Use when callback delayed.
     */
    @GetMapping("/{transactionId}/status")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getStatus(
            @RequestHeader("X-Customer-Id") String externalId,
            @Parameter(description = "Transaction externalId (UUID) from POST /transactions/send response")
            @PathVariable String transactionId) {
        if (transactionFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Transaction service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        ChoiceBankResult result = transactionFacade.queryTransactionStatus(customer.getId(), transactionId);
        return ResponseEntity.ok(ApiSuccessResponses.ok("TXN_STATUS_OK",
                ChoiceCustomerMessage.prefer(result.msg(), "Transaction status retrieved."),
                (Map<String, Object>) result.data()));
    }

    /**
     * Fetches bank codes from Choice Bank for transfer UI (payee bank selection).
     */
    @GetMapping("/bank-codes")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getBankCodes() {
        if (transactionFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Transaction service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        ChoiceBankResult result = transactionFacade.getBankCodes();
        return ResponseEntity.ok(ApiSuccessResponses.ok("TXN_BANK_CODES_OK",
                ChoiceCustomerMessage.prefer(result.msg(), "Bank codes retrieved."),
                (Map<String, Object>) result.data()));
    }

    /**
     * Queries Choice Bank transaction list for the customer's wallet (Choice Bank source of truth).
     * Use for syncing or when local history may be incomplete.
     * Requires {@code kyc_verification.choice_user_id} from the latest KYC row (same id Choice expects as {@code userId}).
     */
    @GetMapping("/choice-history")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getChoiceHistory(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestParam long startTime,
            @RequestParam long endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (transactionFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Transaction service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        var wallet = walletRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Wallet not found", HttpStatus.NOT_FOUND));
        var kycRows = kycVerificationRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        var choiceUserId = kycRows.isEmpty() ? null : kycRows.get(0).getChoiceUserId();
        if (choiceUserId == null || choiceUserId.isBlank()) {
            throw new BusinessException(
                    "CHOICE_USER_ID_MISSING",
                    "Choice user id not available yet; complete onboarding.",
                    HttpStatus.CONFLICT);
        }
        ChoiceBankResult result = transactionFacade.getChoiceTransList(
                wallet.getChoiceAccountId(), choiceUserId, startTime, endTime, page, size);
        return ResponseEntity.ok(ApiSuccessResponses.ok("TXN_CHOICE_HISTORY_OK",
                ChoiceCustomerMessage.prefer(result.msg(), "Choice transaction history retrieved."),
                (Map<String, Object>) result.data()));
    }

    /**
     * Transaction history. Optional filters: status, type.
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> list(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        Specification<Transaction> spec = TransactionSpecification.allOf(
                TransactionSpecification.forCustomer(customer.getId()),
                status != null ? TransactionSpecification.withStatus(status) : null,
                type != null ? TransactionSpecification.withType(type) : null);
        Page<Transaction> txs = transactionRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(txs.map(this::toResponse));
    }

    private TransactionResponse toResponse(TransactionChoiceOutcome outcome) {
        TransactionResponse r = toResponse(outcome.transaction());
        r.setMessage(ChoiceCustomerMessage.prefer(outcome.choiceMsg(), null));
        return r;
    }

    private TransactionResponse toResponse(Transaction tx) {
        var r = new TransactionResponse();
        r.setExternalId(tx.getExternalId());
        r.setType(tx.getType());
        r.setAmount(tx.getAmount());
        r.setCurrency(tx.getCurrency());
        r.setStatus(tx.getStatus());
        r.setDisplayStatus(TransactionDisplayStatus.fromRawStatus(tx.getStatus()));
        r.setPayeeAccountId(tx.getPayeeAccountId());
        r.setPayeeAccountName(tx.getPayeeAccountName());
        r.setPayeeBankCode(tx.getPayeeBankCode());
        r.setRemark(tx.getRemark());
        r.setCreatedAt(tx.getCreatedAt());
        r.setCompletedAt(tx.getCompletedAt());
        return r;
    }
}
