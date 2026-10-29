package com.vycepay.common.apprelease;

/**
 * Server-side update / maintenance decision for a client versionCode.
 */
public record AppReleaseDecision(
        boolean updateRequired,
        boolean updateRecommended,
        boolean maintenance
) {

    /**
     * Computes flags from policy and optional client version code.
     * Missing/invalid versionCode → no update flags (policy fields still returned separately).
     */
    public static AppReleaseDecision from(AppReleasePolicy policy, Integer versionCode) {
        if (policy.maintenance()) {
            return new AppReleaseDecision(false, false, true);
        }
        if (versionCode == null || versionCode < 0) {
            return new AppReleaseDecision(false, false, false);
        }
        if (versionCode < policy.minSupportedVersionCode()) {
            return new AppReleaseDecision(true, true, false);
        }
        if (versionCode < policy.latestVersionCode()) {
            return new AppReleaseDecision(false, true, false);
        }
        return new AppReleaseDecision(false, false, false);
    }
}
