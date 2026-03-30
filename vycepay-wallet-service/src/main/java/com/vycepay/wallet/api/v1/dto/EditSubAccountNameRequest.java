package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class EditSubAccountNameRequest {

    @Schema(description = "Optional sub-account suffix for SME VA; omit to reset to business name only")
    private String subAccountName;

    public String getSubAccountName() {
        return subAccountName;
    }

    public void setSubAccountName(String subAccountName) {
        this.subAccountName = subAccountName;
    }
}
