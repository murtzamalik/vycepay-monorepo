package com.vycepay.kyc.application.dto;

/**
 * Normalized KYC profile captured at onboarding submit and persisted locally.
 */
public record KycProfileCommand(
        String firstName,
        String middleName,
        String lastName,
        String birthday,
        Integer gender,
        String idType,
        String idNumber,
        String address,
        String kraPin,
        String email) {
}
