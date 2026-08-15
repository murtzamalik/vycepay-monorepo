package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single contact mobile from the device address book.
 */
@Schema(description = "Contact mobile in any common Kenya dial format")
public class ContactMobileDto {

    @Schema(description = "Raw mobile from contacts (with or without country code)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "0712345678")
    private String mobile;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
