package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request for registration - triggers OTP send.
 */
@Schema(description = "Registration request; triggers OTP send to mobile")
public class RegisterRequest {

    @Schema(description = "Mobile country code (e.g. 254 for Kenya)", requiredMode = Schema.RequiredMode.REQUIRED, example = "254")
    private String mobileCountryCode;

    @Schema(description = "Mobile number without country code", requiredMode = Schema.RequiredMode.REQUIRED, example = "712345678")
    private String mobile;

    public String getMobileCountryCode() {
        return mobileCountryCode;
    }

    public void setMobileCountryCode(String mobileCountryCode) {
        this.mobileCountryCode = mobileCountryCode;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
