package com.vycepay.wallet.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Confirms mobile number change (Choice account/confirmMobileChange).
 */
public class ConfirmMobileChangeRequest {

    @Schema(description = "Request id from mobile change apply response")
    private String requestId;

    @Schema(description = "OTP sent to the previous phone number")
    private String proveIdCode;

    @Schema(description = "OTP sent to the new phone number")
    private String confirmChangeCode;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProveIdCode() {
        return proveIdCode;
    }

    public void setProveIdCode(String proveIdCode) {
        this.proveIdCode = proveIdCode;
    }

    public String getConfirmChangeCode() {
        return confirmChangeCode;
    }

    public void setConfirmChangeCode(String confirmChangeCode) {
        this.confirmChangeCode = confirmChangeCode;
    }
}
