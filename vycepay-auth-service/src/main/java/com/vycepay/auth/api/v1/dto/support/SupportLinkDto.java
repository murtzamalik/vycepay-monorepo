package com.vycepay.auth.api.v1.dto.support;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * External link for help, legal, or marketing pages.
 */
@Schema(description = "Clickable support or legal link")
public record SupportLinkDto(
        @Schema(description = "Display label", example = "Privacy Policy")
        String title,
        @Schema(description = "HTTPS URL", example = "https://vycepay.com/privacy")
        String url,
        @Schema(description = "Optional link type hint for the client", example = "PRIVACY")
        String type
) {
}
