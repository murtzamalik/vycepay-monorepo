package com.vycepay.kyc.application.service;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vycepay.common.exception.BusinessException;
import com.vycepay.kyc.domain.model.Customer;
import com.vycepay.kyc.infrastructure.persistence.CustomerRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for credential set / overwrite on KYC submit.
 */
@ExtendWith(MockitoExtension.class)
class OnboardingCredentialsServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private OnboardingCredentialsService service;

    @BeforeEach
    void setUp() {
        service = new OnboardingCredentialsService(customerRepository, passwordEncoder);
    }

    @Test
    void overwrite_differentUsername_updatesCredentialsAndClearsLockout() {
        Customer customer = customerWithCredentials(1L, "olduser", "old-hash");
        customer.setPinFailedAttempts(5);
        customer.setPinLockedUntil(Instant.now().plusSeconds(600));
        when(customerRepository.existsByUsernameNormalizedAndIdNot("newuser", 1L)).thenReturn(false);
        when(passwordEncoder.encode("4321")).thenReturn("new-hash");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ensureCredentialsForSubmit(customer, "newuser", "4321", true);

        assertEquals("newuser", customer.getUsername());
        assertEquals("newuser", customer.getUsernameNormalized());
        assertEquals("new-hash", customer.getPinHash());
        assertEquals(0, customer.getPinFailedAttempts());
        assertNull(customer.getPinLockedUntil());
        verify(customerRepository).save(customer);
    }

    @Test
    void overwrite_sameUsername_updatesPinHash() {
        Customer customer = customerWithCredentials(1L, "mohadz", "old-hash");
        when(customerRepository.existsByUsernameNormalizedAndIdNot("mohadz", 1L)).thenReturn(false);
        when(passwordEncoder.encode("9999")).thenReturn("new-hash");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ensureCredentialsForSubmit(customer, "mohadz", "9999", true);

        assertEquals("new-hash", customer.getPinHash());
        verify(customerRepository).save(customer);
    }

    @Test
    void noOverwrite_differentUsername_throwsCredentialsAlreadySet() {
        Customer customer = customerWithCredentials(1L, "olduser", "old-hash");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ensureCredentialsForSubmit(customer, "newuser", "4321", false));

        assertEquals("CREDENTIALS_ALREADY_SET", ex.getCode());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void noOverwrite_sameUsername_isNoOp() {
        Customer customer = customerWithCredentials(1L, "mohadz", "old-hash");

        service.ensureCredentialsForSubmit(customer, "mohadz", "9999", false);

        assertEquals("old-hash", customer.getPinHash());
        verify(passwordEncoder, never()).encode(any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void overwrite_usernameTakenByOther_throwsUsernameTaken() {
        Customer customer = customerWithCredentials(1L, "olduser", "old-hash");
        when(customerRepository.existsByUsernameNormalizedAndIdNot("taken", 1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ensureCredentialsForSubmit(customer, "taken", "4321", true));

        assertEquals("USERNAME_TAKEN", ex.getCode());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void firstTime_setsCredentials() {
        Customer customer = new Customer();
        customer.setId(2L);
        when(customerRepository.existsByUsernameNormalizedAndIdNot("fresh", 2L)).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("hash-1234");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.ensureCredentialsForSubmit(customer, "fresh", "1234", false);

        assertEquals("fresh", customer.getUsernameNormalized());
        assertEquals("hash-1234", customer.getPinHash());
        assertTrue(customer.hasCredentials());
        verify(customerRepository).save(eq(customer));
    }

    private static Customer customerWithCredentials(Long id, String username, String pinHash) {
        Customer c = new Customer();
        c.setId(id);
        c.setUsername(username);
        c.setUsernameNormalized(username.toLowerCase());
        c.setPinHash(pinHash);
        return c;
    }
}
