package com.vycepay.transaction.api.v1.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * List envelope for beneficiaries (pagination-ready).
 */
@Schema(description = "Beneficiary list")
public class BeneficiaryListResponse {

    @Schema(description = "Active beneficiaries, newest updated first")
    private List<BeneficiaryResponse> items;

    public BeneficiaryListResponse() {
    }

    public BeneficiaryListResponse(List<BeneficiaryResponse> items) {
        this.items = items;
    }

    public List<BeneficiaryResponse> getItems() {
        return items;
    }

    public void setItems(List<BeneficiaryResponse> items) {
        this.items = items;
    }
}
