package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public mobile app config / version gate payload.
 */
@Schema(description = "App release policy and update flags for the calling client")
public record AppConfigResponse(
        @Schema(description = "ANDROID or IOS") String platform,
        @Schema(description = "Latest published versionCode") int latestVersionCode,
        @Schema(description = "Latest versionName label") String latestVersionName,
        @Schema(description = "Minimum versionCode allowed to use the app") int minSupportedVersionCode,
        @Schema(description = "True when client must update before continuing") boolean updateRequired,
        @Schema(description = "True when a newer version is available (soft)") boolean updateRecommended,
        @Schema(description = "Short update screen title") String updateTitle,
        @Schema(description = "Customer-facing update body") String updateMessage,
        @Schema(description = "Play Store / App Store URL") String storeUrl,
        @Schema(description = "Sideload APK URL when used") String apkUrl,
        @Schema(description = "True when app is under maintenance") boolean maintenance,
        @Schema(description = "Maintenance screen message") String maintenanceMessage
) {
}
