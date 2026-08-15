package com.vycepay.auth.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One matched VycePay customer from contact verify.
 */
@Schema(description = "Matched contact with ACTIVE wallet")
public class ContactMatchDto {

    @Schema(description = "Original mobile string from the request that produced this match")
    private String inputMobile;

    @Schema(description = "Normalized country code", example = "254")
    private String mobileCountryCode;

    @Schema(description = "Normalized national mobile", example = "712345678")
    private String mobile;

    @Schema(description = "VycePay username")
    private String username;

    @Schema(description = "Display title from first + last name")
    private String accountTitle;

    @Schema(description = "Customer public UUID")
    private String customerExternalId;

    @Schema(description = "Choice account id for later Choice (accountType 0) transfer")
    private String payeeAccountId;

    public ContactMatchDto() {
    }

    public ContactMatchDto(String inputMobile, String mobileCountryCode, String mobile,
                           String username, String accountTitle, String customerExternalId,
                           String payeeAccountId) {
        this.inputMobile = inputMobile;
        this.mobileCountryCode = mobileCountryCode;
        this.mobile = mobile;
        this.username = username;
        this.accountTitle = accountTitle;
        this.customerExternalId = customerExternalId;
        this.payeeAccountId = payeeAccountId;
    }

    public String getInputMobile() {
        return inputMobile;
    }

    public void setInputMobile(String inputMobile) {
        this.inputMobile = inputMobile;
    }

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccountTitle() {
        return accountTitle;
    }

    public void setAccountTitle(String accountTitle) {
        this.accountTitle = accountTitle;
    }

    public String getCustomerExternalId() {
        return customerExternalId;
    }

    public void setCustomerExternalId(String customerExternalId) {
        this.customerExternalId = customerExternalId;
    }

    public String getPayeeAccountId() {
        return payeeAccountId;
    }

    public void setPayeeAccountId(String payeeAccountId) {
        this.payeeAccountId = payeeAccountId;
    }
}
