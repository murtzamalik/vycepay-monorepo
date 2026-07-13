package com.vycepay.auth.api.v1.dto.support;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Grouped FAQ section for help centre navigation.
 */
@Schema(description = "FAQ category with items")
public record FaqCategoryDto(
        @Schema(description = "Category id", example = "account")
        String id,
        @Schema(description = "Category title", example = "Account & verification")
        String title,
        @Schema(description = "FAQ items in this category")
        List<FaqItemDto> items
) {
}
