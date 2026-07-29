package com.vycepay.kyc.application.service;

import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vycepay.common.exception.BusinessException;
import com.vycepay.kyc.domain.model.Customer;
import com.vycepay.kyc.infrastructure.persistence.CustomerRepository;

/**
 * Sets username + PIN during KYC submit (onboarding).
 * Rules match auth-service PinCredentialsService; never logs raw PIN.
 * Same-username retry is a no-op so Choice failures can be retried safely.
 */
@Service
public class OnboardingCredentialsService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._]{2,19}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{4}$");
    private static final Set<String> RESERVED = Set.of(
            "admin", "support", "vycepay", "root", "system", "null", "undefined");

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public OnboardingCredentialsService(CustomerRepository customerRepository,
                                        PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Ensures credentials for onboarding submit.
     * <ul>
     *   <li>Not set → validate and persist</li>
     *   <li>Already set with same username → no-op</li>
     *   <li>Already set with different username → conflict</li>
     * </ul>
     *
     * @param customer loaded customer entity (managed)
     * @param username app login username
     * @param pin      4-digit app login PIN (never logged)
     */
    public void ensureCredentials(Customer customer, String username, String pin) {
        if (username == null || username.isBlank() || pin == null || pin.isBlank()) {
            throw new BusinessException("CREDENTIALS_REQUIRED",
                    "Username and PIN are required for KYC submit", HttpStatus.BAD_REQUEST);
        }
        validateUsernameFormat(username);
        validatePinFormat(pin);
        String normalized = username.toLowerCase();

        if (customer.hasCredentials()) {
            if (normalized.equals(customer.getUsernameNormalized())) {
                return;
            }
            throw new BusinessException("CREDENTIALS_ALREADY_SET",
                    "Credentials already set", HttpStatus.CONFLICT);
        }

        if (customerRepository.existsByUsernameNormalized(normalized)) {
            throw new BusinessException("USERNAME_TAKEN",
                    "Username is already taken", HttpStatus.CONFLICT);
        }

        customer.setUsername(username.trim());
        customer.setUsernameNormalized(normalized);
        customer.setPinHash(passwordEncoder.encode(pin));
        customer.setCredentialsSetAt(Instant.now());
        customerRepository.save(customer);
    }

    private void validateUsernameFormat(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException("USERNAME_INVALID",
                    "Username must be 3–20 chars, start with a letter, and use only letters, digits, . or _",
                    HttpStatus.BAD_REQUEST);
        }
        if (RESERVED.contains(username.toLowerCase())) {
            throw new BusinessException("USERNAME_INVALID", "Username is reserved", HttpStatus.BAD_REQUEST);
        }
    }

    private void validatePinFormat(String pin) {
        if (!PIN_PATTERN.matcher(pin).matches()) {
            throw new BusinessException("INVALID_CREDENTIALS",
                    "PIN must be exactly 4 digits", HttpStatus.BAD_REQUEST);
        }
    }
}
