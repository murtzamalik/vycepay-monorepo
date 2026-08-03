package com.vycepay.admin.api.v1;

import java.util.Map;

import com.vycepay.admin.api.v1.dto.AdminRequests.CustomerStatusRequest;
import com.vycepay.admin.application.service.AdminMutationService;
import com.vycepay.admin.application.service.AdminReadService;
import com.vycepay.admin.application.service.CsvExportService;
import com.vycepay.admin.application.service.RateLimitService;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer backoffice APIs with server-side PII masking and audited status changes. */
@RestController
@RequestMapping("/api/admin/v1/customers")
@PreAuthorize("hasAuthority('PERM_customer:view')")
public class CustomerController {
    private final AdminReadService r;
    private final AdminMutationService m;
    private final CsvExportService csv;
    private final RateLimitService rateLimitService;

    public CustomerController(AdminReadService r, AdminMutationService m, CsvExportService csv, RateLimitService rateLimitService) {
        this.r = r;
        this.m = m;
        this.csv = csv;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("CUSTOMERS_OK", "Customers",
                r.customers(page, size, search, status, fromDate, toDate, sort, order)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PERM_customer:export')")
    public ResponseEntity<String> export(
            HttpServletRequest req,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        rateLimitService.check("export", req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customers.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.customers(req, search, status, fromDate, toDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("CUSTOMER_OK", "Customer", r.customerDetail(id)));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> summary(@PathVariable String id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("CUSTOMER_SUMMARY_OK", "Customer summary", r.customerSummary(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_customer:suspend')")
    public ResponseEntity<ApiSuccessResponse<Void>> status(
            @PathVariable String id, @Valid @RequestBody CustomerStatusRequest body, HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        m.updateCustomerStatus(id, body, req);
        return ResponseEntity.ok(ApiSuccessResponses.ok("CUSTOMER_STATUS_UPDATED", "Customer status updated"));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> tx(
            @PathVariable String id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("CUSTOMER_TX_OK", "Customer transactions",
                r.customerTransactions(id, page, size, type, status, fromDate, toDate, sort, order)));
    }

    @GetMapping("/{id}/kyc")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> kyc(@PathVariable String id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("CUSTOMER_KYC_OK", "Customer KYC", r.customerKyc(id)));
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> act(
            @PathVariable String id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("CUSTOMER_ACTIVITY_OK", "Customer activity",
                r.customerActivity(id, page, size, sort, order)));
    }
}
