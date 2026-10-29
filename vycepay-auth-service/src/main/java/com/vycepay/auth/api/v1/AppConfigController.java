package com.vycepay.auth.api.v1;

import com.vycepay.auth.api.v1.dto.AppConfigResponse;
import com.vycepay.auth.application.config.AppConfigProvider;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public app version / maintenance config for mobile cold start.
 * BFF must permit GET /api/v1/auth/app-config without JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "App config", description = "Mobile version gate and maintenance flags")
public class AppConfigController {

    private final AppConfigProvider appConfigProvider;

    public AppConfigController(AppConfigProvider appConfigProvider) {
        this.appConfigProvider = appConfigProvider;
    }

    /**
     * Returns release policy and update flags for the calling app build.
     */
    @GetMapping("/app-config")
    @Operation(summary = "Get app release config (public)")
    public ResponseEntity<ApiSuccessResponse<AppConfigResponse>> appConfig(
            @RequestHeader(value = "X-Platform", required = false) String platform,
            @RequestHeader(value = "X-App-Version-Code", required = false) String versionCode,
            @RequestHeader(value = "X-App-Version-Name", required = false) String versionName,
            @RequestHeader(value = "X-Distribution", required = false) String distribution) {
        AppConfigResponse data = appConfigProvider.get(platform, versionCode);
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "APP_CONFIG_OK", "App configuration retrieved.", data));
    }
}
