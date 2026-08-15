package com.vycepay.auth.application.service;

import com.vycepay.auth.api.v1.dto.ContactMatchDto;
import com.vycepay.auth.api.v1.dto.ContactMobileDto;
import com.vycepay.auth.api.v1.dto.VerifyContactsRequest;
import com.vycepay.auth.api.v1.dto.VerifyContactsResponse;
import com.vycepay.auth.config.AuthProperties;
import com.vycepay.auth.domain.model.Customer;
import com.vycepay.auth.domain.model.Wallet;
import com.vycepay.auth.infrastructure.persistence.CustomerRepository;
import com.vycepay.auth.infrastructure.persistence.WalletRepository;
import com.vycepay.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for contacts verify matching rules.
 */
@ExtendWith(MockitoExtension.class)
class ContactsVerifyServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private WalletRepository walletRepository;

    private ContactsVerifyService service;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        AuthProperties.Rule rule = new AuthProperties.Rule();
        rule.setEnabled(false);
        props.getRateLimit().getPolicies().put("contacts_verify", rule);
        AuthRateLimitService rateLimitService = new AuthRateLimitService(props);
        service = new ContactsVerifyService(customerRepository, walletRepository, rateLimitService);
    }

    @Test
    void verify_returnsMatchWithActiveWallet() {
        Customer caller = caller(1L, "caller-ext", "700000001");
        when(customerRepository.findByExternalId("caller-ext")).thenReturn(Optional.of(caller));

        Customer peer = peer(2L, "peer-ext", "712345678", "jane_doe", "Jane", "Doe");
        when(customerRepository.findWithUsernameByCountryAndMobilesExcluding(
                eq("254"), anyCollection(), eq(1L))).thenReturn(List.of(peer));

        Wallet wallet = new Wallet();
        wallet.setCustomerId(2L);
        wallet.setChoiceAccountId("46012001327585");
        wallet.setStatus(Wallet.STATUS_ACTIVE);
        when(walletRepository.findByCustomerIdInAndStatus(eq(List.of(2L)), eq(Wallet.STATUS_ACTIVE)))
                .thenReturn(List.of(wallet));

        VerifyContactsResponse res = service.verify("caller-ext", request("0798765432", "0712345678"));

        assertEquals(1, res.getMatches().size());
        ContactMatchDto m = res.getMatches().get(0);
        assertEquals("0712345678", m.getInputMobile());
        assertEquals("254", m.getMobileCountryCode());
        assertEquals("712345678", m.getMobile());
        assertEquals("jane_doe", m.getUsername());
        assertEquals("Jane Doe", m.getAccountTitle());
        assertEquals("peer-ext", m.getCustomerExternalId());
        assertEquals("46012001327585", m.getPayeeAccountId());
    }

    @Test
    void verify_excludesSelfEvenIfInList() {
        Customer caller = caller(1L, "caller-ext", "712345678");
        caller.setUsername("me");
        caller.setFirstName("Me");
        when(customerRepository.findByExternalId("caller-ext")).thenReturn(Optional.of(caller));
        when(customerRepository.findWithUsernameByCountryAndMobilesExcluding(
                eq("254"), anyCollection(), eq(1L))).thenReturn(List.of());

        VerifyContactsResponse res = service.verify("caller-ext", request("0712345678"));
        assertTrue(res.getMatches().isEmpty());
        verify(walletRepository, never()).findByCustomerIdInAndStatus(any(), anyString());
    }

    @Test
    void verify_skipsCustomerWithoutActiveWallet() {
        Customer caller = caller(1L, "caller-ext", "700000001");
        when(customerRepository.findByExternalId("caller-ext")).thenReturn(Optional.of(caller));

        Customer peer = peer(2L, "peer-ext", "712345678", "jane_doe", "Jane", "Doe");
        when(customerRepository.findWithUsernameByCountryAndMobilesExcluding(
                eq("254"), anyCollection(), eq(1L))).thenReturn(List.of(peer));
        when(walletRepository.findByCustomerIdInAndStatus(eq(List.of(2L)), eq(Wallet.STATUS_ACTIVE)))
                .thenReturn(List.of());

        VerifyContactsResponse res = service.verify("caller-ext", request("0712345678"));
        assertTrue(res.getMatches().isEmpty());
    }

    @Test
    void verify_emptyContacts_throws() {
        Customer caller = caller(1L, "caller-ext", "700000001");
        when(customerRepository.findByExternalId("caller-ext")).thenReturn(Optional.of(caller));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verify("caller-ext", request()));
        assertEquals("INVALID_CONTACTS", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void verify_overMax_throws() {
        Customer caller = caller(1L, "caller-ext", "700000001");
        when(customerRepository.findByExternalId("caller-ext")).thenReturn(Optional.of(caller));

        VerifyContactsRequest req = new VerifyContactsRequest();
        List<ContactMobileDto> list = new ArrayList<>();
        for (int i = 0; i < ContactsVerifyService.MAX_CONTACTS + 1; i++) {
            ContactMobileDto dto = new ContactMobileDto();
            dto.setMobile("71234567" + (i % 10));
            list.add(dto);
        }
        req.setContacts(list);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verify("caller-ext", req));
        assertEquals("INVALID_CONTACTS", ex.getCode());
        verify(customerRepository, never()).findWithUsernameByCountryAndMobilesExcluding(
                anyString(), anyCollection(), anyLong());
    }

    @Test
    void verify_allInvalidMobiles_returnsEmptyMatches() {
        Customer caller = caller(1L, "caller-ext", "700000001");
        when(customerRepository.findByExternalId("caller-ext")).thenReturn(Optional.of(caller));

        VerifyContactsResponse res = service.verify("caller-ext", request("abc", "123"));
        assertTrue(res.getMatches().isEmpty());
        verify(customerRepository, never()).findWithUsernameByCountryAndMobilesExcluding(
                anyString(), anyCollection(), anyLong());
    }

    @Test
    void buildAccountTitle_dropsBlankParts() {
        assertEquals("Jane Doe", ContactsVerifyService.buildAccountTitle(" Jane ", " Doe "));
        assertEquals("Jane", ContactsVerifyService.buildAccountTitle("Jane", null));
        assertEquals("Doe", ContactsVerifyService.buildAccountTitle("  ", "Doe"));
        assertEquals("", ContactsVerifyService.buildAccountTitle(null, null));
    }

    private static Customer caller(long id, String externalId, String mobile) {
        Customer c = new Customer();
        c.setId(id);
        c.setExternalId(externalId);
        c.setMobileCountryCode("254");
        c.setMobile(mobile);
        return c;
    }

    private static Customer peer(long id, String externalId, String mobile,
                                 String username, String first, String last) {
        Customer c = caller(id, externalId, mobile);
        c.setUsername(username);
        c.setFirstName(first);
        c.setLastName(last);
        return c;
    }

    private static VerifyContactsRequest request(String... mobiles) {
        VerifyContactsRequest req = new VerifyContactsRequest();
        List<ContactMobileDto> list = new ArrayList<>();
        for (String m : mobiles) {
            ContactMobileDto dto = new ContactMobileDto();
            dto.setMobile(m);
            list.add(dto);
        }
        req.setContacts(list);
        return req;
    }
}
