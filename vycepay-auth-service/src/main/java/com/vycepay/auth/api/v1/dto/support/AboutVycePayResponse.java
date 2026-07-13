package com.vycepay.auth.api.v1.dto.support;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * About VycePay screen payload aligned with vycepay.com positioning.
 */
@Schema(description = "About VycePay content")
public record AboutVycePayResponse(
        @Schema(description = "Product name", example = "VycePay")
        String appName,
        @Schema(description = "Primary tagline from brand")
        String tagline,
        @Schema(description = "One-line product description")
        String description,
        @Schema(description = "Ordered content sections")
        List<AboutSectionDto> sections,
        @Schema(description = "Trust and product highlights")
        List<AboutHighlightDto> highlights,
        @Schema(description = "Website and legal links")
        List<SupportLinkDto> links,
        @Schema(description = "Banking partner name", example = "Choice Bank")
        String bankingPartner,
        @Schema(description = "Regulatory disclaimer for wallet users")
        String regulatoryDisclaimer,
        @Schema(description = "Copyright line")
        String copyright
) {
}
