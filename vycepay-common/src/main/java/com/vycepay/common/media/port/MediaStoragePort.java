package com.vycepay.common.media.port;

/**
 * Stores KYC biometric media (images/videos) outside MySQL.
 * Implementations: local filesystem (dev/default) or S3-compatible object storage.
 */
public interface MediaStoragePort {

    /**
     * Persists bytes under a generated key within {@code keyPrefix}.
     *
     * @param keyPrefix logical folder e.g. {@code kyc/{customerId}/{sessionId}}
     * @param contentType MIME type
     * @param bytes       raw media
     * @param fileHint    optional filename hint (e.g. id-front.jpg)
     * @return durable object key (not a public URL)
     */
    String store(String keyPrefix, String contentType, byte[] bytes, String fileHint);

    /**
     * Loads object bytes by key.
     */
    byte[] load(String objectKey);

    /** @return true if object exists */
    boolean exists(String objectKey);
}
