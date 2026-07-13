package com.vycepay.auth.api.v1.dto.support;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contact channel shown on help centre (email, phone, chat).
 */
@Schema(description = "Support contact channel")
public record SupportContactDto(
        @Schema(description = "Channel type", example = "EMAIL")
        String type,
        @Schema(description = "Display label", example = "Email support")
        String label,
        @Schema(description = "Contact value (email, phone, or deep link)", example = "support@vycepay.com")
        String value,
        @Schema(description = "Optional availability note", example = "Mon–Fri, 8:00–18:00 EAT")
        String hours
) {
}
