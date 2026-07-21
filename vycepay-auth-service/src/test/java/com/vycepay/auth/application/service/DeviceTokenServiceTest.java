package com.vycepay.auth.application.service;

import com.vycepay.auth.domain.model.DeviceToken;
import com.vycepay.auth.infrastructure.persistence.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private DeviceTokenService deviceTokenService;

    @BeforeEach
    void setUp() {
        deviceTokenService = new DeviceTokenService(deviceTokenRepository);
    }

    @Test
    void replaceToken_deletesAllThenInsertsOne() {
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceTokenService.replaceTokenForCustomer(42L, "fcm-abc", "ANDROID");

        verify(deviceTokenRepository).deleteByCustomerId(42L);
        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        DeviceToken saved = captor.getValue();
        assertEquals(42L, saved.getCustomerId());
        assertEquals("fcm-abc", saved.getFcmToken());
        assertEquals("ANDROID", saved.getPlatform());
    }

    @Test
    void replaceToken_defaultsPlatformToAndroid() {
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
    }

    @Test
    void replaceToken_nullCustomer_isNoOp() {
        deviceTokenService.replaceTokenForCustomer(null, "tok", "ANDROID");
        verify(deviceTokenRepository, never()).deleteByCustomerId(any());
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void clearTokens_deletesByCustomerId() {
        deviceTokenService.clearTokensForCustomer(99L);
        verify(deviceTokenRepository).deleteByCustomerId(99L);
    }

    @Test
    void clearTokens_nullCustomer_isNoOp() {
        deviceTokenService.clearTokensForCustomer(null);
        verify(deviceTokenRepository, never()).deleteByCustomerId(any());
    }
}
