package com.vycepay.admin.api.v1;

import java.util.Map;

import com.vycepay.admin.application.service.AdminReadService;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** KYC backoffice list and detail APIs. */
@RestController
@RequestMapping("/api/admin/v1/kyc")
@PreAuthorize("hasAuthority('PERM_kyc:view')")
public class KycController {
    private final AdminReadService s;

    public KycController(AdminReadService s) {
        this.s = s;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("KYC_OK", "KYC",
                s.kyc(page, size, status, search, fromDate, toDate, sort, order)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("KYC_DETAIL_OK", "KYC detail", s.kycDetail(id)));
    }
}
