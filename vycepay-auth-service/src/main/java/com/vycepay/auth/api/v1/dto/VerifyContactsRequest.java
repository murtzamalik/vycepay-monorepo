package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch contact verify request — raw mobiles from the device address book.
 */
@Schema(description = "List of contact mobiles to match against VycePay customers")
public class VerifyContactsRequest {

    @Schema(description = "Contacts to verify (max 500)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ContactMobileDto> contacts = new ArrayList<>();

    public List<ContactMobileDto> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactMobileDto> contacts) {
        this.contacts = contacts;
    }
}
