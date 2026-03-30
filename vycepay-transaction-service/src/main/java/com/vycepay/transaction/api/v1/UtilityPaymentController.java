package com.vycepay.transaction.api.v1;

import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.transaction.api.v1.dto.TransactionResponse;
import com.vycepay.transaction.application.facade.UtilityPaymentFacade;
import com.vycepay.transaction.domain.model.Transaction;
import com.vycepay.transaction.domain.model.TransactionDisplayStatus;
import com.vycepay.transaction.infrastructure.persistence.CustomerRepository;
import com.vycepay.transaction.infrastructure.persistence.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Choice Bank utility payment APIs (airtime, bill query/pay, payment queries).
 */
@RestController
@RequestMapping("/api/v1/transactions/utilities")
public class UtilityPaymentController {

    private final UtilityPaymentFacade utilityPaymentFacade;
    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;

    public UtilityPaymentController(
            @Autowired(required = false) UtilityPaymentFacade utilityPaymentFacade,
            CustomerRepository customerRepository,
            WalletRepository walletRepository) {
        this.utilityPaymentFacade = utilityPaymentFacade;
        this.customerRepository = customerRepository;
        this.walletRepository = walletRepository;
    }

    @PostMapping("/airtime")
    public ResponseEntity<ApiSuccessResponse<TransactionResponse>> airtime(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Map<String, Object> body) {
        requireFacade();
        var ctx = loadWallet(externalId);
        Transaction tx = utilityPaymentFacade.airtimePayment(
                ctx.customerId(), ctx.walletId(), ctx.choiceAccountId(), body, idempotencyKey);
        return ResponseEntity.ok(ApiSuccessResponses.ok("UTILITY_AIRTIME_INITIATED",
                "Airtime payment initiated.", toResponse(tx)));
    }

    @PostMapping("/airtime-bulk")
    public ResponseEntity<ApiSuccessResponse<TransactionResponse>> airtimeBulk(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Map<String, Object> body) {
        requireFacade();
        var ctx = loadWallet(externalId);
        Transaction tx = utilityPaymentFacade.airtimeBulkPayment(
                ctx.customerId(), ctx.walletId(), ctx.choiceAccountId(), body, idempotencyKey);
        return ResponseEntity.ok(ApiSuccessResponses.ok("UTILITY_AIRTIME_BULK_INITIATED",
                "Bulk airtime payment initiated.", toResponse(tx)));
    }

    @PostMapping("/bill-query")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> billQuery(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody Map<String, Object> body) {
        requireFacade();
        var ctx = loadWallet(externalId);
        Map<String, Object> params = new HashMap<>(body);
        Map<String, Object> data = utilityPaymentFacade.billQuery(ctx.choiceAccountId(), params);
        return ResponseEntity.ok(ApiSuccessResponses.ok("UTILITY_BILL_QUERY_OK", "Bill query completed.", data));
    }

    @PostMapping("/bill-payment")
    public ResponseEntity<ApiSuccessResponse<TransactionResponse>> billPayment(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Map<String, Object> body) {
        requireFacade();
        var ctx = loadWallet(externalId);
        Transaction tx = utilityPaymentFacade.billPayment(
                ctx.customerId(), ctx.walletId(), ctx.choiceAccountId(), body, idempotencyKey);
        return ResponseEntity.ok(ApiSuccessResponses.ok("UTILITY_BILL_PAYMENT_INITIATED",
                "Bill payment initiated.", toResponse(tx)));
    }

    @PostMapping("/payment-query")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> paymentQuery(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody Map<String, Object> body) {
        requireFacade();
        var ctx = loadWallet(externalId);
        Map<String, Object> data = utilityPaymentFacade.paymentQuery(ctx.choiceAccountId(), body);
        return ResponseEntity.ok(ApiSuccessResponses.ok("UTILITY_PAYMENT_QUERY_OK", "Payment query completed.", data));
    }

    @PostMapping("/bulk-payment-query")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> bulkPaymentQuery(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody Map<String, Object> body) {
        requireFacade();
        var ctx = loadWallet(externalId);
        Map<String, Object> data = utilityPaymentFacade.bulkPaymentQuery(ctx.choiceAccountId(), body);
        return ResponseEntity.ok(ApiSuccessResponses.ok("UTILITY_BULK_PAYMENT_QUERY_OK",
                "Bulk payment query completed.", data));
    }

    private WalletCtx loadWallet(String externalId) {
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        var wallet = walletRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Wallet not found", HttpStatus.NOT_FOUND));
        return new WalletCtx(customer.getId(), wallet.getId(), wallet.getChoiceAccountId());
    }

    private void requireFacade() {
        if (utilityPaymentFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "Choice Bank is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
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

    private record WalletCtx(Long customerId, Long walletId, String choiceAccountId) {
    }
}
