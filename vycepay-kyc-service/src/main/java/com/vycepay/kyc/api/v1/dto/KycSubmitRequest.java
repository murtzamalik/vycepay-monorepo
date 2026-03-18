package com.vycepay.kyc.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Request for KYC submission. Fields match Choice Bank submitEasyOnboardingRequest params.
 */
@Schema(description = "KYC onboarding submission request")
public class KycSubmitRequest {

    @Schema(description = "First name")
    private String firstName;
    @Schema(description = "Middle name")
    private String middleName;
    @Schema(description = "Last name")
    private String lastName;
    @Schema(description = "Birthday (YYYY-MM-DD)")
    private String birthday;
    @Schema(description = "Gender code: 0=Female, 1=Male (Choice Bank native). Also accepts 2=Female for backward compatibility.")
    private Integer gender;
    @Schema(description = "Country code (default 254)")
    private String countryCode;
    @Schema(description = "Mobile number")
    private String mobile;
    @Schema(description = "ID type (101=National ID, 102=Alien, 103=Passport)")
    private String idType;
    @Schema(description = "ID number")
    private String idNumber;
    @Schema(description = "Address (optional for easy onboarding)")
    private String address;
    @Schema(description = "KRA PIN (optional)")
    private String kraPin;
    @Schema(description = "Email (optional)")
    private String email;
    @Schema(description = "Base64 front side of ID")
    private String frontSidePhoto;
    @Schema(description = "Base64 back side of ID")
    private String backSidePhoto;
    @Schema(description = "Base64 selfie photo")
    private String selfiePhoto;

    /**
     * Builds Choice Bank params. Optional fields added only when present.
     */
    public Map<String, Object> toChoiceParams(String userId) {
        var params = new java.util.HashMap<String, Object>();
        params.put("userId", userId);
        params.put("firstName", firstName != null ? firstName : "");
        params.put("middleName", middleName != null ? middleName : "");
        params.put("lastName", lastName != null ? lastName : "");
        params.put("birthday", birthday != null ? birthday : "");
        params.put("gender", mapGenderToChoiceBank(gender));
        params.put("countryCode", countryCode != null ? countryCode : "254");
        params.put("mobile", mobile != null ? mobile : "");
        params.put("idType", idType != null ? idType : "101");
        params.put("idNumber", idNumber != null ? idNumber : "");
        params.put("frontSidePhoto", frontSidePhoto != null ? frontSidePhoto : "");
        if (backSidePhoto != null && !backSidePhoto.isBlank()) params.put("backSidePhoto", backSidePhoto);
        params.put("selfiePhoto", selfiePhoto != null ? selfiePhoto : "");
        if (address != null) params.put("address", address);
        if (kraPin != null) params.put("kraPin", kraPin);
        if (email != null) params.put("email", email);
        return params;
    }

    /**
     * Maps gender to Choice Bank codes: 0=Female, 1=Male.
     * Accepts 0, 1 (native) or 2 (Female) for backward compatibility.
     */
    private int mapGenderToChoiceBank(Integer gender) {
        if (gender == null) return 1;
        if (gender == 0) return 0;  // Female
        if (gender == 2) return 0;  // Legacy: 2 meant Female
        return 1;  // Male (1 or any other)
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getKraPin() {
        return kraPin;
    }

    public void setKraPin(String kraPin) {
        this.kraPin = kraPin;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFrontSidePhoto() {
        return frontSidePhoto;
    }

    public void setFrontSidePhoto(String frontSidePhoto) {
        this.frontSidePhoto = frontSidePhoto;
    }

    public String getBackSidePhoto() {
        return backSidePhoto;
    }

    public void setBackSidePhoto(String backSidePhoto) {
        this.backSidePhoto = backSidePhoto;
    }

    public String getSelfiePhoto() {
        return selfiePhoto;
    }

    public void setSelfiePhoto(String selfiePhoto) {
        this.selfiePhoto = selfiePhoto;
    }
}
