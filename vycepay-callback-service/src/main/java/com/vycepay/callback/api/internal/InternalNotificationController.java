package com.vycepay.callback.api.internal;

import com.vycepay.callback.application.notification.NotificationOrchestrator;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Internal notification APIs for admin-service (compose / resend).
 * Protected by {@code X-Internal-Api-Key}.
 */
@RestController
@RequestMapping("/internal/v1/notifications")
public class InternalNotificationController {

    private final NotificationOrchestrator notificationOrchestrator;

    public InternalNotificationController(NotificationOrchestrator notificationOrchestrator) {
        this.notificationOrchestrator = notificationOrchestrator;
    }

    @PostMapping("/compose")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> compose(
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId,
            @Valid @RequestBody ComposeRequest body) {
        Map<String, Object> result = notificationOrchestrator.compose(
                body.customerIds(), body.title(), body.body(), body.data(), adminId);
        return ResponseEntity.ok(ApiSuccessResponses.ok("NOTIFICATION_COMPOSED", "Notifications composed", result));
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> resend(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId) {
        Map<String, Object> result = notificationOrchestrator.resend(id, adminId);
        return ResponseEntity.ok(ApiSuccessResponses.ok("NOTIFICATION_RESENT", "Notification resent", result));
    }

    public record ComposeRequest(
            @NotEmpty @Size(max = 100) List<Long> customerIds,
            @NotBlank @Size(max = 128) String title,
            @NotBlank @Size(max = 512) String body,
            Map<String, String> data
    ) {}
}
