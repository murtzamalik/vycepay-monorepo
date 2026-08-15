package com.vycepay.auth.application.service;

import com.vycepay.auth.api.v1.dto.ContactMatchDto;
import com.vycepay.auth.api.v1.dto.ContactMobileDto;
import com.vycepay.auth.api.v1.dto.VerifyContactsRequest;
import com.vycepay.auth.api.v1.dto.VerifyContactsResponse;
import com.vycepay.auth.domain.model.Customer;
import com.vycepay.auth.domain.model.Wallet;
import com.vycepay.auth.infrastructure.persistence.CustomerRepository;
import com.vycepay.auth.infrastructure.persistence.WalletRepository;
import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Matches device contact mobiles against VycePay customers with ACTIVE wallets.
 * Thread-safe; does not persist contact lists.
 */
@Service
public class ContactsVerifyService {

    public static final int MAX_CONTACTS = 500;

    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final AuthRateLimitService rateLimitService;

    public ContactsVerifyService(CustomerRepository customerRepository,
                                 WalletRepository walletRepository,
                                 AuthRateLimitService rateLimitService) {
        this.customerRepository = customerRepository;
        this.walletRepository = walletRepository;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Normalizes contact mobiles and returns matches that have username + ACTIVE wallet.
     *
     * @param callerExternalId authenticated customer externalId from BFF
     * @param request          raw contact list from the app
     * @return matches only (unmatched / ineligible omitted)
     * @throws BusinessException if contacts missing, empty, or over max size
     */
    public VerifyContactsResponse verify(String callerExternalId, VerifyContactsRequest request) {
        rateLimitService.check("contacts_verify", callerExternalId);

        Customer caller = customerRepository.findByExternalId(callerExternalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));

        if (request == null || request.getContacts() == null) {
            throw new BusinessException("INVALID_CONTACTS", "contacts is required", HttpStatus.BAD_REQUEST);
        }
        List<ContactMobileDto> contacts = request.getContacts();
        if (contacts.isEmpty()) {
            throw new BusinessException("INVALID_CONTACTS", "contacts must not be empty", HttpStatus.BAD_REQUEST);
        }
        if (contacts.size() > MAX_CONTACTS) {
            throw new BusinessException("INVALID_CONTACTS",
                    "contacts must not exceed " + MAX_CONTACTS, HttpStatus.BAD_REQUEST);
        }

        // key -> first raw input that normalized to that key
        Map<String, String> inputByKey = new LinkedHashMap<>();
        Map<String, MobileNormalizer.NormalizedMobile> normalizedByKey = new LinkedHashMap<>();
        for (ContactMobileDto item : contacts) {
            if (item == null || item.getMobile() == null) {
                continue;
            }
            String raw = item.getMobile();
            MobileNormalizer.normalize(raw).ifPresent(n -> {
                if (!normalizedByKey.containsKey(n.key())) {
                    normalizedByKey.put(n.key(), n);
                    inputByKey.put(n.key(), raw.trim());
                }
            });
        }

        if (normalizedByKey.isEmpty()) {
            return new VerifyContactsResponse(List.of());
        }

        // Kenya-first: all keys share DEFAULT_COUNTRY_CODE after normalize
        List<String> mobiles = normalizedByKey.values().stream()
                .map(MobileNormalizer.NormalizedMobile::mobile)
                .distinct()
                .toList();

        List<Customer> candidates = customerRepository.findWithUsernameByCountryAndMobilesExcluding(
                MobileNormalizer.DEFAULT_COUNTRY_CODE, mobiles, caller.getId());

        if (candidates.isEmpty()) {
            return new VerifyContactsResponse(List.of());
        }

        List<Long> candidateIds = candidates.stream().map(Customer::getId).toList();
        Map<Long, Wallet> activeWallets = walletRepository
                .findByCustomerIdInAndStatus(candidateIds, Wallet.STATUS_ACTIVE)
                .stream()
                .collect(Collectors.toMap(Wallet::getCustomerId, w -> w, (a, b) -> a));

        List<ContactMatchDto> matches = new ArrayList<>();
        Map<String, Customer> byKey = new HashMap<>();
        for (Customer c : candidates) {
            byKey.put(c.getMobileCountryCode() + ":" + c.getMobile(), c);
        }

        for (Map.Entry<String, MobileNormalizer.NormalizedMobile> entry : normalizedByKey.entrySet()) {
            String key = entry.getKey();
            Customer matched = byKey.get(key);
            if (matched == null) {
                continue;
            }
            Wallet wallet = activeWallets.get(matched.getId());
            if (wallet == null || wallet.getChoiceAccountId() == null || wallet.getChoiceAccountId().isBlank()) {
                continue;
            }
            matches.add(new ContactMatchDto(
                    inputByKey.get(key),
                    matched.getMobileCountryCode(),
                    matched.getMobile(),
                    matched.getUsername(),
                    buildAccountTitle(matched.getFirstName(), matched.getLastName()),
                    matched.getExternalId(),
                    wallet.getChoiceAccountId()));
        }

        return new VerifyContactsResponse(matches);
    }

    /**
     * Builds display title from first and last name; blank parts dropped.
     */
    static String buildAccountTitle(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }
}
