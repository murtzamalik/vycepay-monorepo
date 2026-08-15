package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Contact verify result — matched customers only.
 */
@Schema(description = "Matched VycePay contacts with ACTIVE wallets")
public class VerifyContactsResponse {

    @Schema(description = "Contacts that matched registered customers with ACTIVE wallets")
    private List<ContactMatchDto> matches = new ArrayList<>();

    public VerifyContactsResponse() {
    }

    public VerifyContactsResponse(List<ContactMatchDto> matches) {
        this.matches = matches != null ? matches : new ArrayList<>();
    }

    public List<ContactMatchDto> getMatches() {
        return matches;
    }

    public void setMatches(List<ContactMatchDto> matches) {
        this.matches = matches;
    }
}
