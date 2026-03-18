package com.vycepay.common.security.port;

/**
 * Port for encrypting/decrypting sensitive data at rest.
 * Production implementations should use AES or similar; default is no-op for dev.
 */
public interface SensitiveDataEncryptionPort {

    /**
     * Encrypts plaintext for storage. Returns base64 or hex string.
     *
     * @param plaintext Sensitive value (e.g. ID number, mobile)
     * @return Encrypted string for DB storage, or plaintext if no-op
     */
    String encrypt(String plaintext);

    /**
     * Decrypts stored value for use in application.
     *
     * @param ciphertext Value from DB (encrypted or plain if no-op)
     * @return Decrypted plaintext
     */
    String decrypt(String ciphertext);
}
