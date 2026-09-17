package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Category row in the Kenya utilities catalog.
 */
@Schema(description = "Utility biller category")
public class UtilityBillerCategoryDto {

    @Schema(description = "Category id", example = "ELECTRICITY")
    private String id;

    @Schema(description = "Display name", example = "Electricity")
    private String name;

    public UtilityBillerCategoryDto() {
    }

    public UtilityBillerCategoryDto(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
