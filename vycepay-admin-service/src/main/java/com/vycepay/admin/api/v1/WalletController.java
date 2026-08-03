package com.vycepay.admin.api.v1;

import java.util.Map;

import com.vycepay.admin.api.v1.dto.AdminRequests.WalletStatusRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Wallet backoffice APIs with audited freeze/unfreeze actions. */
@RestController
@RequestMapping("/api/admin/v1/wallets")
@PreAuthorize("hasAuthority('PERM_wallet:view')")
public class WalletController {
    private final AdminReadService r;
    private final AdminMutationService m;
    private final RateLimitService rateLimitService;

    public WalletController(AdminReadService r, AdminMutationService m, RateLimitService rateLimitService) {
        this.r = r;
        this.m = m;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("WALLETS_OK", "Wallets",
                r.wallets(page, size, search, status, sort, order)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("WALLET_OK", "Wallet", r.walletDetail(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_wallet:freeze')")
    public ResponseEntity<ApiSuccessResponse<Void>> status(
            @PathVariable Long id, @Valid @RequestBody WalletStatusRequest body, HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        m.updateWalletStatus(id, body, req);
        return ResponseEntity.ok(ApiSuccessResponses.ok("WALLET_STATUS_UPDATED", "Wallet status updated"));
    }
}
