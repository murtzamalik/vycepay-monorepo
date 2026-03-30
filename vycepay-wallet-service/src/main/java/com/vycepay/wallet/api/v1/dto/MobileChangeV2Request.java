package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class MobileChangeV2Request {

    @Schema(description = "New mobile country code, e.g. 254")
    private String newMobileCountryCode;

    @Schema(description = "New mobile number without country code")
    private String newMobileNumber;

    public String getNewMobileCountryCode() {
        return newMobileCountryCode;
    }

    public void setNewMobileCountryCode(String newMobileCountryCode) {
        this.newMobileCountryCode = newMobileCountryCode;
    }

    public String getNewMobileNumber() {
        return newMobileNumber;
    }

    public void setNewMobileNumber(String newMobileNumber) {
        this.newMobileNumber = newMobileNumber;
    }
}
