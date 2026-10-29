package com.vycepay.common.apprelease;

/**
 * Resolves platform policy (DB active row, else defaults) and update decisions.
 */
public class AppReleasePolicyService {

    private final AppReleasePolicyPort port;

    public AppReleasePolicyService(AppReleasePolicyPort port) {
        this.port = port;
    }

    public AppReleasePolicy resolve(String platform) {
        return port.findActiveByPlatform(platform)
                .orElseGet(() -> AppReleasePolicyDefaults.forPlatform(platform));
    }

    public AppReleaseDecision decide(AppReleasePolicy policy, Integer versionCode) {
        return AppReleaseDecision.from(policy, versionCode);
    }
}
