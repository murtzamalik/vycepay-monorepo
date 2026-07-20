package com.vycepay.callback.infrastructure.push;

import com.vycepay.callback.domain.model.PushMessage;
import com.vycepay.callback.infrastructure.persistence.DeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Ensures Firebase adapter no-ops when disabled (no FCM calls / no token lookup required for safety).
 */
@ExtendWith(MockitoExtension.class)
class FirebasePushAdapterTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    void disabled_doesNotTouchRepository() {
        FirebasePushAdapter adapter = new FirebasePushAdapter(deviceTokenRepository, null, false);
        PushMessage message = PushMessage.builder()
                .title("t")
                .body("b")
                .pushType("TRANSACTION_RESULT")
                .notificationType("0002")
                .build();

        adapter.sendToCustomer(42L, message);

        verify(deviceTokenRepository, never()).findByCustomerId(any());
    }
}
