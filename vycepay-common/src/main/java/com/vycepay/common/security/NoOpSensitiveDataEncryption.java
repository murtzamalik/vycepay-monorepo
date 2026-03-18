package com.vycepay.common.security;

import com.vycepay.common.security.port.SensitiveDataEncryptionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * No-op implementation for development. Does not encrypt; use real impl in production.
 */
@Component
@ConditionalOnMissingBean(SensitiveDataEncryptionPort.class)
public class NoOpSensitiveDataEncryption implements SensitiveDataEncryptionPort {

    @Override
    public String encrypt(String plaintext) {
        return plaintext;
    }

    @Override
    public String decrypt(String ciphertext) {
        return ciphertext;
    }
}
