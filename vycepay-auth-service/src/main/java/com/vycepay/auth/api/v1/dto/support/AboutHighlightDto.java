package com.vycepay.auth.api.v1.dto.support;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Trust or product highlight on the About screen.
 */
@Schema(description = "Product highlight card")
public record AboutHighlightDto(
        @Schema(description = "Icon key for the client", example = "shield")
        String icon,
        @Schema(description = "Card title", example = "Regulated & secure")
        String title,
        @Schema(description = "Short description")
        String description
) {
}
