package com.vycepay.kyc.api.v1.dto.biometric;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Create biometric KYC session")
public class CreateBiometricSessionRequest {
    private String idType;
    private String documentCountry;
    private Boolean forceNew;

    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }
    public String getDocumentCountry() { return documentCountry; }
    public void setDocumentCountry(String documentCountry) { this.documentCountry = documentCountry; }
    public Boolean getForceNew() { return forceNew; }
    public void setForceNew(Boolean forceNew) { this.forceNew = forceNew; }
}
