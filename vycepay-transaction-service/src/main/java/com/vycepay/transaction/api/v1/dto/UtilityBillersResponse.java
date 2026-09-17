package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response for GET /api/v1/transactions/utilities/billers.
 */
@Schema(description = "Kenya utilities catalog")
public class UtilityBillersResponse {

    @Schema(description = "Category labels for UI grouping")
    private List<UtilityBillerCategoryDto> categories;

    @Schema(description = "Billers (optionally filtered by category)")
    private List<UtilityBillerDto> billers;

    public UtilityBillersResponse() {
    }

    public UtilityBillersResponse(List<UtilityBillerCategoryDto> categories, List<UtilityBillerDto> billers) {
        this.categories = categories;
        this.billers = billers;
    }

    public List<UtilityBillerCategoryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<UtilityBillerCategoryDto> categories) {
        this.categories = categories;
    }

    public List<UtilityBillerDto> getBillers() {
        return billers;
    }

    public void setBillers(List<UtilityBillerDto> billers) {
        this.billers = billers;
    }
}
