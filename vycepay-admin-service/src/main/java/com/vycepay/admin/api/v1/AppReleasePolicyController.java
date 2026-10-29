package com.vycepay.admin.api.v1;

import com.vycepay.admin.api.v1.dto.AdminRequests.AppReleasePolicyUpdateRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for mobile app release policy (version gate + maintenance).
 */
@RestController
@RequestMapping("/api/admin/v1/app-release-policies")
@PreAuthorize("hasAuthority('PERM_mobile:release:view')")
public class AppReleasePolicyController {

    private final AdminReadService readService;
    private final AdminMutationService mutationService;
    private final RateLimitService rateLimitService;

    public AppReleasePolicyController(AdminReadService readService,
                                      AdminMutationService mutationService,
                                      RateLimitService rateLimitService) {
        this.readService = readService;
        this.mutationService = mutationService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<Map<String, Object>>>> list() {
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "APP_RELEASE_POLICIES_OK", "App release policies",
                readService.appReleasePolicies()));
    }

    @GetMapping("/{platform}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> detail(@PathVariable String platform) {
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "APP_RELEASE_POLICY_OK", "App release policy",
                readService.appReleasePolicyDetail(platform)));
    }

    @PutMapping("/{platform}")
    @PreAuthorize("hasAuthority('PERM_mobile:release:edit')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> update(
            @PathVariable String platform,
            @Valid @RequestBody AppReleasePolicyUpdateRequest body,
            HttpServletRequest req) {
        rateLimitService.check("mutation", req);
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "APP_RELEASE_POLICY_UPDATED", "App release policy updated",
                mutationService.updateAppReleasePolicy(platform, body, req)));
    }
}
