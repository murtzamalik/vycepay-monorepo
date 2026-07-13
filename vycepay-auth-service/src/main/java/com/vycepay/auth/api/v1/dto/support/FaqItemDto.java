package com.vycepay.auth.api.v1.dto.support;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single FAQ entry for help centre accordion UI.
 */
@Schema(description = "FAQ question and answer")
public record FaqItemDto(
        @Schema(description = "Stable id for analytics", example = "vycescore-explained")
        String id,
        @Schema(description = "Question shown in the UI")
        String question,
        @Schema(description = "Answer body (plain text; paragraphs separated by blank lines)")
        String answer
) {
}
