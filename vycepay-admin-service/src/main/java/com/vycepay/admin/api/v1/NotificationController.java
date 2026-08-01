package com.vycepay.admin.api.v1;

import java.util.Map;

import com.vycepay.admin.api.v1.dto.AdminRequests.NotificationComposeRequest;
import com.vycepay.admin.api.v1.dto.AdminRequests.NotificationResendRequest;
import com.vycepay.admin.application.service.AdminMutationService;
import com.vycepay.admin.application.service.AdminReadService;
import com.vycepay.admin.application.service.RateLimitService;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
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
 * Admin notification inbox visibility, resend, and compose APIs.
 */
@RestController
@RequestMapping("/api/admin/v1/notifications")
@PreAuthorize("hasAuthority('PERM_notification:view')")
public class NotificationController {

    private final AdminReadService readService;
    private final AdminMutationService mutationService;
    private final RateLimitService rateLimitService;

    public NotificationController(AdminReadService readService,
                                  AdminMutationService mutationService,
                                  RateLimitService rateLimitService) {
        this.readService = readService;
        this.mutationService = mutationService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String pushType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("NOTIFICATIONS_OK", "Notifications",
                readService.notifications(page, size, customerId, pushType, source, batchId, fromDate, toDate)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiSuccessResponses.ok("NOTIFICATION_SUMMARY_OK", "Notification summary",
                readService.notificationSummary()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("NOTIFICATION_OK", "Notification",
                readService.notificationDetail(id)));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAuthority('PERM_notification:resend')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> resend(
            @PathVariable Long id,
            @Valid @RequestBody NotificationResendRequest body,
            HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        return ResponseEntity.ok(ApiSuccessResponses.ok("NOTIFICATION_RESENT", "Notification resent",
                mutationService.resendNotification(id, body, req)));
    }

    @PostMapping("/compose")
    @PreAuthorize("hasAuthority('PERM_notification:compose')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> compose(
            @Valid @RequestBody NotificationComposeRequest body,
            HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        return ResponseEntity.ok(ApiSuccessResponses.ok("NOTIFICATION_COMPOSED", "Notifications composed",
                mutationService.composeNotification(body, req)));
    }
}
