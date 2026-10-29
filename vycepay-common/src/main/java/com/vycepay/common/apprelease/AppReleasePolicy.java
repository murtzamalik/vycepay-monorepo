package com.vycepay.common.apprelease;

/**
 * Active (or default) release policy for one mobile platform.
 */
public record AppReleasePolicy(
        String platform,
        int minSupportedVersionCode,
        int latestVersionCode,
        String latestVersionName,
        String updateTitle,
        String updateMessage,
        String storeUrl,
        String apkUrl,
        boolean maintenance,
        String maintenanceMessage,
        boolean active
) {
}
