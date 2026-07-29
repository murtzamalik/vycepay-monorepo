package com.vycepay.auth.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vycepay.auth.domain.model.DeviceToken;
import com.vycepay.auth.infrastructure.persistence.DeviceTokenRepository;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for single-device FCM token replace/clear policy.
 */
@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private EntityManager entityManager;

    private DeviceTokenService deviceTokenService;

    @BeforeEach
    void setUp() {
        deviceTokenService = new DeviceTokenService(deviceTokenRepository, entityManager);
    }

    @Test
    void replaceToken_insertsWhenNoneExist() {
        when(deviceTokenRepository.findByCustomerIdAndFcmToken(42L, "fcm-abc")).thenReturn(Optional.empty());
        when(deviceTokenRepository.findByCustomerId(42L)).thenReturn(Collections.emptyList());
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceTokenService.replaceTokenForCustomer(42L, "fcm-abc", "ANDROID");

        verify(entityManager).flush();
        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        DeviceToken saved = captor.getValue();
        assertEquals(42L, saved.getCustomerId());
        assertEquals("fcm-abc", saved.getFcmToken());
        assertEquals("ANDROID", saved.getPlatform());
    }

    @Test
    void replaceToken_sameToken_updatesPlatformOnly() {
        DeviceToken existing = new DeviceToken();
        existing.setId(10L);
        existing.setCustomerId(42L);
        existing.setFcmToken("fcm-abc");
        existing.setPlatform("IOS");
        when(deviceTokenRepository.findByCustomerIdAndFcmToken(42L, "fcm-abc")).thenReturn(Optional.of(existing));
        when(deviceTokenRepository.findByCustomerId(42L)).thenReturn(List.of(existing));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceTokenService.replaceTokenForCustomer(42L, "fcm-abc", "ANDROID");

        verify(entityManager).flush();
        verify(deviceTokenRepository, never()).delete(any());
        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        assertEquals("ANDROID", captor.getValue().getPlatform());
        assertEquals(10L, captor.getValue().getId());
    }

    @Test
    void replaceToken_defaultsPlatformToAndroid() {
        when(deviceTokenRepository.findByCustomerIdAndFcmToken(7L, "tok")).thenReturn(Optional.empty());
        when(deviceTokenRepository.findByCustomerId(7L)).thenReturn(Collections.emptyList());
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceTokenService.replaceTokenForCustomer(7L, "tok", null);

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        assertEquals("ANDROID", captor.getValue().getPlatform());
    }

    @Test
    void replaceToken_blankFcm_isNoOp() {
        deviceTokenService.replaceTokenForCustomer(1L, "  ", "ANDROID");
        deviceTokenService.replaceTokenForCustomer(1L, null, "ANDROID");

        verifyNoInteractions(deviceTokenRepository);
        verifyNoInteractions(entityManager);
    }

    @Test
    void replaceToken_nullCustomer_isNoOp() {
        deviceTokenService.replaceTokenForCustomer(null, "tok", "ANDROID");
        verify(deviceTokenRepository, never()).findByCustomerId(any());
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void clearTokens_deletesByCustomerId() {
        deviceTokenService.clearTokensForCustomer(99L);
        verify(deviceTokenRepository).deleteByCustomerId(99L);
        verify(entityManager).flush();
    }

    @Test
    void clearTokens_nullCustomer_isNoOp() {
        deviceTokenService.clearTokensForCustomer(null);
        verify(deviceTokenRepository, never()).deleteByCustomerId(any());
    }
}
