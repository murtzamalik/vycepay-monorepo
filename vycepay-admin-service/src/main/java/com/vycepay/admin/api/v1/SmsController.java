package com.vycepay.admin.api.v1;

import java.util.Map;

import com.vycepay.admin.api.v1.dto.AdminRequests.SmsBulkRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.SmsResendRequest;
import com.vycepay.admin.application.service.AdminMutationService;
import com.vycepay.admin.application.service.AdminReadService;
import com.vycepay.admin.application.service.RateLimitService;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.sms.port.SmsBalanceResult;
import com.vycepay.common.sms.port.SmsPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin SMS ledger visibility, resend, bulk send, and balance APIs.
 */
@RestController
@RequestMapping("/api/admin/v1/sms")
@PreAuthorize("hasAuthority('PERM_sms:view')")
public class SmsController {

    private final AdminReadService readService;
    private final AdminMutationService mutationService;
    private final RateLimitService rateLimitService;
    private final SmsPort smsPort;

    public SmsController(AdminReadService readService,
                         AdminMutationService mutationService,
                         RateLimitService rateLimitService,
                         SmsPort smsPort) {
        this.readService = readService;
        this.mutationService = mutationService;
        this.rateLimitService = rateLimitService;
        this.smsPort = smsPort;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_LIST_OK", "SMS messages",
                readService.smsMessages(page, size, status, purpose, recipient, batchId, fromDate, toDate, sort, order)));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> balance() {
        SmsBalanceResult result = smsPort.balance();
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("success", result.success());
        data.put("data", result.data());
        data.put("errorMessage", result.errorMessage());
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_BALANCE_OK", "SMS balance", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_OK", "SMS message",
                readService.smsDetail(id)));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAuthority('PERM_sms:resend')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> resend(
            @PathVariable Long id,
            @Valid @RequestBody SmsResendRequest body,
            HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_RESENT", "SMS resent",
                mutationService.resendSms(id, body, req)));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_sms:bulk')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> bulk(
            @Valid @RequestBody SmsBulkRequest body,
            HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_BULK_SENT", "Bulk SMS submitted",
                mutationService.bulkSms(body, req)));
    }
}
