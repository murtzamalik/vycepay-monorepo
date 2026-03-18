package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Current customer profile returned by GET /api/v1/auth/me.
 */
@Schema(description = "Customer profile")
public class CustomerProfileResponse {

    @Schema(description = "External customer ID (UUID)")
    private String externalId;

    @Schema(description = "Mobile country code (e.g. 254)")
    private String mobileCountryCode;

    @Schema(description = "Mobile number")
    private String mobile;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Account status")
    private String status;

    public CustomerProfileResponse() {
    }

    public CustomerProfileResponse(String externalId, String mobileCountryCode, String mobile,
                                   String firstName, String lastName, String email, String status) {
        this.externalId = externalId;
        this.mobileCountryCode = mobileCountryCode;
        this.mobile = mobile;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
    }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getMobileCountryCode() { return mobileCountryCode; }
    public void setMobileCountryCode(String mobileCountryCode) { this.mobileCountryCode = mobileCountryCode; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
