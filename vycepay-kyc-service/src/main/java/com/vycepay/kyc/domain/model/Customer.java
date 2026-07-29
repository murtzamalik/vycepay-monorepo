package com.vycepay.kyc.domain.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Customer reference. Same table as auth service.
 * Credential columns are written during KYC submit (onboarding) so username/PIN
 * commit only with a successful onboarding transaction.
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Column(name = "mobile_country_code", nullable = false)
    private String mobileCountryCode;

    @Column(name = "mobile", nullable = false)
    private String mobile;

    @Column(name = "username", length = 20)
    private String username;

    @Column(name = "username_normalized", length = 20)
    private String usernameNormalized;

    @Column(name = "pin_hash", length = 100)
    private String pinHash;

    @Column(name = "credentials_set_at")
    private Instant credentialsSetAt;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    /**
     * @return true when both username and PIN hash are present
     */
    public boolean hasCredentials() {
        return pinHash != null && !pinHash.isBlank() && username != null && !username.isBlank();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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

    public String getUsernameNormalized() {
        return usernameNormalized;
    }

    public void setUsernameNormalized(String usernameNormalized) {
        this.usernameNormalized = usernameNormalized;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public Instant getCredentialsSetAt() {
        return credentialsSetAt;
    }

    public void setCredentialsSetAt(Instant credentialsSetAt) {
        this.credentialsSetAt = credentialsSetAt;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
