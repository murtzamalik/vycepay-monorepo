package com.vycepay.transaction.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Single Kenya utility / paybill entry for mobile catalog UI.
 */
@Schema(description = "Kenya utility biller (paybill metadata for M-Pesa business transfer)")
public class UtilityBillerDto {

    @Schema(description = "Stable biller id", example = "kplc-prepaid")
    private String id;

    @Schema(description = "Display name")
    private String name;

    @Schema(description = "Category id (ELECTRICITY, WATER, HEALTHCARE, INTERNET)")
    private String category;

    @Schema(description = "Primary M-Pesa paybill / business number", example = "888880")
    private String paybill;

    @Schema(description = "Alternate paybill numbers when applicable")
    private List<String> alternatePaybills;

    @Schema(description = "Hint for the account / reference field the user must enter")
    private String accountHint;

    @Schema(description = "Extra operator notes for the UI")
    private String notes;

    @Schema(description = "Send-money accountType for this biller (always 1 = M-Pesa business/paybill)")
    private int accountType = 1;

    public UtilityBillerDto() {
    }

    public UtilityBillerDto(String id, String name, String category, String paybill,
                            List<String> alternatePaybills, String accountHint, String notes) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.paybill = paybill;
        this.alternatePaybills = alternatePaybills;
        this.accountHint = accountHint;
        this.notes = notes;
        this.accountType = 1;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPaybill() {
        return paybill;
    }

    public void setPaybill(String paybill) {
        this.paybill = paybill;
    }

    public List<String> getAlternatePaybills() {
        return alternatePaybills;
    }

    public void setAlternatePaybills(List<String> alternatePaybills) {
        this.alternatePaybills = alternatePaybills;
    }

    public String getAccountHint() {
        return accountHint;
    }

    public void setAccountHint(String accountHint) {
        this.accountHint = accountHint;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getAccountType() {
        return accountType;
    }

    public void setAccountType(int accountType) {
        this.accountType = accountType;
    }
}
