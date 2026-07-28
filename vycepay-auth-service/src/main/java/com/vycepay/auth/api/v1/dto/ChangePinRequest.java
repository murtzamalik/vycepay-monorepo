package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Change PIN while authenticated.
 */
@Schema(description = "Change PIN with old PIN verification")
public class ChangePinRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPin;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPin;

    public String getOldPin() {
        return oldPin;
    }

    public void setOldPin(String oldPin) {
        this.oldPin = oldPin;
    }

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }
}
