package com.vycepay.admin.api.v1;

import com.vycepay.admin.api.v1.dto.AdminRequests.SmsBulkRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.SmsResendRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.SmsTemplatePreviewRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.SmsTemplateUpdateRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/outbox")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> outboxList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String dedupeKey,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_OUTBOX_LIST_OK", "SMS outbox",
                readService.smsOutbox(page, size, status, recipient, dedupeKey, customerId,
                        fromDate, toDate, sort, order)));
    }

    @GetMapping("/outbox/{id}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> outboxDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_OUTBOX_OK", "SMS outbox row",
                readService.smsOutboxDetail(id)));
    }

    @PostMapping("/outbox/{id}/retry")
    @PreAuthorize("hasAuthority('PERM_sms:resend')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> outboxRetry(
            @PathVariable Long id,
            @Valid @RequestBody SmsResendRequest body,
            HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_OUTBOX_QUEUED", "SMS outbox requeued",
                mutationService.retrySmsOutbox(id, body, req)));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiSuccessResponse<List<Map<String, Object>>>> templates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_TEMPLATES_OK", "SMS templates",
                readService.smsTemplates(category, active)));
    }

    @GetMapping("/templates/{key}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> templateDetail(@PathVariable String key) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_TEMPLATE_OK", "SMS template",
                readService.smsTemplateDetail(key)));
    }

    @PutMapping("/templates/{key}")
    @PreAuthorize("hasAuthority('PERM_sms:template:edit')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> templateUpdate(
            @PathVariable String key,
            @Valid @RequestBody SmsTemplateUpdateRequest body,
            HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_TEMPLATE_UPDATED", "SMS template updated",
                mutationService.updateSmsTemplate(key, body, req)));
    }

    @PostMapping("/templates/{key}/preview")
    @PreAuthorize("hasAuthority('PERM_sms:view')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> templatePreview(
            @PathVariable String key,
            @Valid @RequestBody SmsTemplatePreviewRequest body) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_TEMPLATE_PREVIEW_OK", "SMS template preview",
                mutationService.previewSmsTemplate(key, body)));
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

    /**
     * Preview eligible ALL_CUSTOMERS counts (ACTIVE/PENDING/SUSPENDED with valid Kenya mobile).
     */
    @GetMapping("/bulk/audience-preview")
    @PreAuthorize("hasAuthority('PERM_sms:bulk')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> bulkAudiencePreview() {
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_BULK_AUDIENCE_PREVIEW",
                "Bulk SMS audience preview", mutationService.bulkSmsAudiencePreview()));
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
