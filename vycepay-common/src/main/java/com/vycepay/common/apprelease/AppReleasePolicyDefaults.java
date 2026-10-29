package com.vycepay.common.apprelease;

/**
 * Safe compile-time fallback when DB row is missing or inactive.
 * Matches V17 seed (versionCode 1) so existing Drive installs are not force-blocked.
 */
public final class AppReleasePolicyDefaults {

    public static final String PLATFORM_ANDROID = "ANDROID";
    public static final String PLATFORM_IOS = "IOS";

    private AppReleasePolicyDefaults() {
    }

    public static String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return PLATFORM_ANDROID;
        }
        String p = platform.trim().toUpperCase();
        if ("IOS".equals(p) || "IPHONE".equals(p) || "IPADOS".equals(p)) {
            return PLATFORM_IOS;
        }
        return PLATFORM_ANDROID;
    }

    public static boolean isKnownPlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return false;
        }
        String p = platform.trim().toUpperCase();
        return PLATFORM_ANDROID.equals(p) || PLATFORM_IOS.equals(p);
    }

    public static AppReleasePolicy forPlatform(String platform) {
        String p = normalizePlatform(platform);
        String storeUrl = PLATFORM_ANDROID.equals(p)
                ? "https://play.google.com/store/apps/details?id=com.wallet.vycepay"
                : null;
        return new AppReleasePolicy(
                p,
                1,
                1,
                "1.0.0",
                "Update available",
                "Please update VycePay to continue.",
                storeUrl,
                null,
                false,
                null,
                true
        );
    }
}
