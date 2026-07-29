package com.vycepay.auth.application.service;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vycepay.auth.config.AuthProperties;
import com.vycepay.auth.domain.model.Customer;
import com.vycepay.auth.infrastructure.persistence.CustomerRepository;
import com.vycepay.common.exception.BusinessException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Username + PIN credential management with lockout.
 * PIN is always stored as BCrypt hash; never logged.
 */
@Service
public class PinCredentialsService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._]{2,19}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{4}$");
    private static final Set<String> RESERVED = Set.of(
            "admin", "support", "vycepay", "root", "system", "null", "undefined");

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final AuthAuditService authAuditService;
    private final AuthMetricsService authMetricsService;

    public PinCredentialsService(CustomerRepository customerRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuthProperties authProperties,
                                 AuthAuditService authAuditService,
                                 AuthMetricsService authMetricsService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        this.authAuditService = authAuditService;
        this.authMetricsService = authMetricsService;
    }

    public void validateUsernameFormat(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException("USERNAME_INVALID",
                    "Username must be 3–20 chars, start with a letter, and use only letters, digits, . or _",
                    HttpStatus.BAD_REQUEST);
        }
        if (RESERVED.contains(username.toLowerCase())) {
            throw new BusinessException("USERNAME_INVALID", "Username is reserved", HttpStatus.BAD_REQUEST);
        }
    }

    public void validatePinFormat(String pin) {
        if (pin == null || !PIN_PATTERN.matcher(pin).matches()) {
            throw new BusinessException("INVALID_CREDENTIALS", "PIN must be exactly 4 digits", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Sets username and PIN once (migrate gate). Same username when already set is a no-op.
     * Onboarding must set credentials via KYC submit, not this method.
     */
    public void setCredentials(Customer customer, String username, String pin) {
        validateUsernameFormat(username);
        validatePinFormat(pin);
        String normalized = username.toLowerCase();
        if (customer.hasCredentials()) {
            if (normalized.equals(customer.getUsernameNormalized())) {
                return;
            }
            throw new BusinessException("CREDENTIALS_ALREADY_SET", "Credentials already set", HttpStatus.CONFLICT);
        }
        if (customerRepository.existsByUsernameNormalized(normalized)) {
            throw new BusinessException("USERNAME_TAKEN", "Username is already taken", HttpStatus.CONFLICT);
        }
        customer.setUsername(username);
        customer.setUsernameNormalized(normalized);
        customer.setPinHash(passwordEncoder.encode(pin));
        customer.setCredentialsSetAt(Instant.now());
        customer.setPinFailedAttempts(0);
        customer.setPinLockedUntil(null);
        customerRepository.save(customer);
        authMetricsService.incrementCredentialsSet();
        authAuditService.record(customer.getId(), "CREDENTIALS_SET", "SUCCESS",
                customer.getUsernameNormalized(), null);
    }

    /**
     * Changes PIN when old PIN matches.
     */
    public void changePin(Customer customer, String oldPin, String newPin) {
        assertNotLocked(customer);
        validatePinFormat(oldPin);
        validatePinFormat(newPin);
        if (customer.getPinHash() == null || !passwordEncoder.matches(oldPin, customer.getPinHash())) {
            registerFailedAttempt(customer);
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        customer.setPinHash(passwordEncoder.encode(newPin));
        customer.setPinFailedAttempts(0);
        customer.setPinLockedUntil(null);
        customerRepository.save(customer);
        authAuditService.record(customer.getId(), "PIN_CHANGE", "SUCCESS",
                customer.getUsernameNormalized(), null);
    }

    /**
     * Resets PIN after OTP verification (forgot-pin confirm).
     */
    public void resetPin(Customer customer, String newPin) {
        validatePinFormat(newPin);
        customer.setPinHash(passwordEncoder.encode(newPin));
        customer.setPinFailedAttempts(0);
        customer.setPinLockedUntil(null);
        if (customer.getCredentialsSetAt() == null) {
            customer.setCredentialsSetAt(Instant.now());
        }
        customerRepository.save(customer);
        authMetricsService.incrementPinReset();
        authAuditService.record(customer.getId(), "PIN_RESET", "SUCCESS",
                OtpService.maskMobile(customer.getMobile()), null);
    }

    /**
     * Verifies PIN; updates lockout counters. Does not check device.
     */
    public void verifyPinOrThrow(Customer customer, String pin) {
        assertNotLocked(customer);
        validatePinFormat(pin);
        if (customer.getPinHash() == null || !passwordEncoder.matches(pin, customer.getPinHash())) {
            registerFailedAttempt(customer);
            authMetricsService.incrementLoginFailure();
            authAuditService.record(customer.getId(), "LOGIN", "FAILURE",
                    maskIdentifier(customer), "invalid_pin");
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        customer.setPinFailedAttempts(0);
        customer.setPinLockedUntil(null);
        customerRepository.save(customer);
    }

    public void assertNotLocked(Customer customer) {
        Instant lockedUntil = customer.getPinLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            authMetricsService.incrementPinLockout();
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Account temporarily locked due to failed PIN attempts", HttpStatus.LOCKED);
        }
    }

    private void registerFailedAttempt(Customer customer) {
        int attempts = customer.getPinFailedAttempts() + 1;
        customer.setPinFailedAttempts(attempts);
        int max = authProperties.getPin().getMaxAttempts();
        if (attempts >= max) {
            customer.setPinLockedUntil(Instant.now().plus(
                    authProperties.getPin().getLockoutMinutes(), ChronoUnit.MINUTES));
            customer.setPinFailedAttempts(0);
            authMetricsService.incrementPinLockout();
            authAuditService.record(customer.getId(), "PIN_LOCKOUT", "LOCKED",
                    maskIdentifier(customer), "max_attempts");
        }
        customerRepository.save(customer);
    }

    private static String maskIdentifier(Customer customer) {
        if (customer.getUsernameNormalized() != null) {
            return customer.getUsernameNormalized();
        }
        return OtpService.maskMobile(customer.getMobile());
    }
}
