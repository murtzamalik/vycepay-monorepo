package com.vycepay.callback.api.v1;

import com.vycepay.callback.application.notification.MobileNotificationService;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Mobile inbox APIs. Customer context comes from BFF-injected {@code X-Customer-Id} (external_id).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final MobileNotificationService mobileNotificationService;

    public NotificationController(MobileNotificationService mobileNotificationService) {
        this.mobileNotificationService = mobileNotificationService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "NOTIFICATIONS_OK", "Notifications", mobileNotificationService.list(externalId, page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> unreadCount(
            @RequestHeader("X-Customer-Id") String externalId) {
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "NOTIFICATION_UNREAD_OK", "Unread count", mobileNotificationService.unreadCount(externalId)));
    }

    @PatchMapping("/{publicId}/read")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> markRead(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable String publicId) {
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "NOTIFICATION_READ_OK", "Marked read", mobileNotificationService.markRead(externalId, publicId)));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable String publicId) {
        mobileNotificationService.softDelete(externalId, publicId);
        return ResponseEntity.ok(ApiSuccessResponses.ok("NOTIFICATION_DELETED", "Notification deleted"));
    }
}
