package com.vycepay.admin.api.v1;

import java.util.Map;

import com.vycepay.admin.application.service.AdminReadService;
import com.vycepay.admin.application.service.CsvExportService;
import com.vycepay.admin.application.service.RateLimitService;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit log APIs for customer activity, auth security events, and admin action trails.
 */
@RestController
@RequestMapping("/api/admin/v1/audit-log")
@PreAuthorize("hasAuthority('PERM_audit_log:view')")
public class AuditLogController {
    private final AdminReadService r;
    private final CsvExportService csv;
    private final RateLimitService rateLimitService;

    public AuditLogController(AdminReadService r, CsvExportService csv, RateLimitService rateLimitService) {
        this.r = r;
        this.csv = csv;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "admin") String source,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        Map<String, Object> data = switch (source) {
            case "auth" -> r.authAuditLog(page, size, action, fromDate, toDate, sort, order);
            case "customer" -> r.auditLog(page, size, action, customerId, fromDate, toDate, sort, order);
            default -> r.adminAuditLog(page, size, action, fromDate, toDate, sort, order);
        };
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUDIT_OK", "Audit log", data));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            HttpServletRequest req,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "admin") String source,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        rateLimitService.check("export", req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-log.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.auditLog(req, source, action, fromDate, toDate));
    }
}
