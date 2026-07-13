package com.vycepay.auth.api.v1.dto.support;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Text section on the About VycePay screen.
 */
@Schema(description = "About screen text section")
public record AboutSectionDto(
        @Schema(description = "Section heading")
        String title,
        @Schema(description = "Body copy (plain text; paragraphs separated by blank lines)")
        String body
) {
}
