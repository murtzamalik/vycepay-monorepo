package com.vycepay.transaction.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vycepay.common.exception.BusinessException;
import com.vycepay.transaction.api.v1.dto.CreateBeneficiaryRequest;
import com.vycepay.transaction.api.v1.dto.UpdateBeneficiaryRequest;
import com.vycepay.transaction.domain.model.Beneficiary;
import com.vycepay.transaction.infrastructure.persistence.BeneficiaryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for beneficiary upsert, soft-delete restore, and validation.
 */
@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    private BeneficiaryService service;

    @BeforeEach
    void setUp() {
        service = new BeneficiaryService(beneficiaryRepository);
    }

    @Test
    void save_createsNewWhenNoMatch() {
        CreateBeneficiaryRequest req = request("Mum", 4, "68", "0123456789", "JANE DOE");
        when(beneficiaryRepository.findByCustomerIdAndAccountTypeAndPayeeBankCodeAndPayeeAccountId(
                1L, 4, "68", "0123456789")).thenReturn(Optional.empty());
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(inv -> inv.getArgument(0));

        BeneficiaryService.SaveResult result = service.save(1L, req);

        assertTrue(result.created());
        assertEquals("Mum", result.response().getNickname());
        assertEquals(4, result.response().getAccountType());
        assertEquals("68", result.response().getPayeeBankCode());
        ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(beneficiaryRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getCustomerId());
    }

    @Test
    void save_upsertsActiveMatch() {
        Beneficiary existing = active(10L, "uuid-1", 1L, 4, "68", "0123456789", "Old");
        CreateBeneficiaryRequest req = request("New Nick", 4, "68", "0123456789", "JANE DOE");
        when(beneficiaryRepository.findByCustomerIdAndAccountTypeAndPayeeBankCodeAndPayeeAccountId(
                1L, 4, "68", "0123456789")).thenReturn(Optional.of(existing));
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(inv -> inv.getArgument(0));

        BeneficiaryService.SaveResult result = service.save(1L, req);

        assertFalse(result.created());
        assertEquals("New Nick", result.response().getNickname());
        assertEquals("JANE DOE", result.response().getPayeeAccountName());
        verify(beneficiaryRepository).save(existing);
    }

    @Test
    void save_restoresSoftDeleted() {
        Beneficiary deleted = active(10L, "uuid-1", 1L, 3, "", "712345678", "Old");
        deleted.setDeletedAt(Instant.now());
        CreateBeneficiaryRequest req = request("Mobile", 3, null, "712345678", "JOHN");
        when(beneficiaryRepository.findByCustomerIdAndAccountTypeAndPayeeBankCodeAndPayeeAccountId(
                1L, 3, "", "712345678")).thenReturn(Optional.of(deleted));
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(inv -> inv.getArgument(0));

        BeneficiaryService.SaveResult result = service.save(1L, req);

        assertFalse(result.created());
        assertEquals(null, deleted.getDeletedAt());
        assertEquals("Mobile", result.response().getNickname());
    }

    @Test
    void save_type4_requiresBankCode() {
        CreateBeneficiaryRequest req = request("X", 4, "  ", "123", null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(1L, req));
        assertEquals("BANK_CODE_REQUIRED", ex.getCode());
        verify(beneficiaryRepository, never()).save(any());
    }

    @Test
    void save_invalidAccountType() {
        CreateBeneficiaryRequest req = request("X", 9, "68", "123", null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(1L, req));
        assertEquals("INVALID_ACCOUNT_TYPE", ex.getCode());
    }

    @Test
    void updateNickname_notFound_throws() {
        when(beneficiaryRepository.findByExternalIdAndCustomerIdAndDeletedAtIsNull("missing", 1L))
                .thenReturn(Optional.empty());
        UpdateBeneficiaryRequest req = new UpdateBeneficiaryRequest();
        req.setNickname("N");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateNickname(1L, "missing", req));
        assertEquals("BENEFICIARY_NOT_FOUND", ex.getCode());
    }

    @Test
    void softDelete_setsDeletedAt() {
        Beneficiary existing = active(10L, "uuid-1", 1L, 0, "", "acc-1", "Name");
        when(beneficiaryRepository.findByExternalIdAndCustomerIdAndDeletedAtIsNull("uuid-1", 1L))
                .thenReturn(Optional.of(existing));
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L, "uuid-1");

        assertTrue(existing.isDeleted());
        verify(beneficiaryRepository).save(existing);
    }

    @Test
    void list_returnsMappedItems() {
        Beneficiary b = active(1L, "u1", 1L, 0, "", "a1", "N");
        when(beneficiaryRepository.findByCustomerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(b));

        var list = service.list(1L);

        assertEquals(1, list.getItems().size());
        assertEquals("u1", list.getItems().get(0).getExternalId());
        verify(beneficiaryRepository).findByCustomerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(eq(1L));
    }

    private static CreateBeneficiaryRequest request(String nick, int type, String bank,
                                                    String accountId, String name) {
        CreateBeneficiaryRequest r = new CreateBeneficiaryRequest();
        r.setNickname(nick);
        r.setAccountType(type);
        r.setPayeeBankCode(bank);
        r.setPayeeAccountId(accountId);
        r.setPayeeAccountName(name);
        return r;
    }

    private static Beneficiary active(Long id, String ext, Long customerId, int type,
                                      String bank, String accountId, String nick) {
        Beneficiary b = new Beneficiary();
        b.setId(id);
        b.setExternalId(ext);
        b.setCustomerId(customerId);
        b.setAccountType(type);
        b.setPayeeBankCode(bank);
        b.setPayeeAccountId(accountId);
        b.setNickname(nick);
        return b;
    }
}
