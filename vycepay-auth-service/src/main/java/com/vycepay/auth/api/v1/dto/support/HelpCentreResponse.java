package com.vycepay.auth.api.v1.dto.support;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Help centre screen payload for the mobile app.
 */
@Schema(description = "Help centre content")
public record HelpCentreResponse(
        @Schema(description = "Screen title", example = "Help & Support")
        String title,
        @Schema(description = "Short intro under the title")
        String subtitle,
        @Schema(description = "Primary support channels")
        List<SupportContactDto> contacts,
        @Schema(description = "Grouped FAQs")
        List<FaqCategoryDto> categories,
        @Schema(description = "Legal and policy links")
        List<SupportLinkDto> legalLinks,
        @Schema(description = "Emergency / fraud reporting note")
        String securityNotice
) {
}
