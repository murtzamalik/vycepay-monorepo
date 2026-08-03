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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Transaction backoffice APIs including failed transaction analysis and audited CSV export. */
@RestController
@RequestMapping("/api/admin/v1/transactions")
@PreAuthorize("hasAuthority('PERM_transaction:view')")
public class TransactionController {
    private final AdminReadService r;
    private final CsvExportService csv;
    private final RateLimitService rateLimitService;

    public TransactionController(AdminReadService r, CsvExportService csv, RateLimitService rateLimitService) {
        this.r = r;
        this.csv = csv;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("TX_OK", "Transactions",
                r.transactions(page, size, type, status, search, customerId, fromDate, toDate, sort, order)));
    }

    @GetMapping("/failed")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> failed(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("FAILED_TX_OK", "Failed transactions",
                r.failedTransactions(page, size, errorCode, fromDate, toDate, sort, order)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PERM_transaction:export')")
    public ResponseEntity<String> export(
            HttpServletRequest req,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        rateLimitService.check("export", req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.transactions(req, type, status, search, fromDate, toDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("TX_DETAIL_OK", "Transaction", r.transactionDetail(id)));
    }
}
