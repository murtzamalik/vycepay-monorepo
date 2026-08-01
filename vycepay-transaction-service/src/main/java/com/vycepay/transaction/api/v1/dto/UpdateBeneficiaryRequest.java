package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to rename a beneficiary.
 */
@Schema(description = "Update beneficiary nickname")
public class UpdateBeneficiaryRequest {

    @Schema(description = "Display nickname (1–50 chars)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
