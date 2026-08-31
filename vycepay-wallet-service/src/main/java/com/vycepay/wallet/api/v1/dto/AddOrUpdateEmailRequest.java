package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Add or replace the customer's email. Identity (onboard type, ID type, document number)
 * is loaded from KYC internally — do not send those fields from the app.
 */
public class AddOrUpdateEmailRequest {

    @Schema(description = "New email address", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "customer@example.com")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
