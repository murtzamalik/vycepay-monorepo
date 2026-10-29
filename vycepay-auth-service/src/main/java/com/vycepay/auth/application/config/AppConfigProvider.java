package com.vycepay.auth.application.config;

import com.vycepay.auth.api.v1.dto.AppConfigResponse;
import com.vycepay.common.apprelease.AppReleaseDecision;
import com.vycepay.common.apprelease.AppReleasePolicy;
import com.vycepay.common.apprelease.AppReleasePolicyService;
import org.springframework.stereotype.Component;

/**
 * Builds public app-config response from DB-backed release policy.
 * Called by AppConfigController for GET /api/v1/auth/app-config.
 */
@Component
public class AppConfigProvider {

    private final AppReleasePolicyService policyService;

    public AppConfigProvider(AppReleasePolicyService policyService) {
        this.policyService = policyService;
    }

    /**
     * @param platformHeader X-Platform (optional)
     * @param versionCodeHeader X-App-Version-Code (optional)
     */
    public AppConfigResponse get(String platformHeader, String versionCodeHeader) {
        AppReleasePolicy policy = policyService.resolve(platformHeader);
        Integer versionCode = parseVersionCode(versionCodeHeader);
        AppReleaseDecision decision = policyService.decide(policy, versionCode);
        return new AppConfigResponse(
                policy.platform(),
                policy.latestVersionCode(),
                policy.latestVersionName(),
                policy.minSupportedVersionCode(),
                decision.updateRequired(),
                decision.updateRecommended(),
                policy.updateTitle(),
                policy.updateMessage(),
                policy.storeUrl(),
                policy.apkUrl(),
                decision.maintenance(),
                policy.maintenanceMessage()
        );
    }

    private static Integer parseVersionCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
