package com.vycepay.common.media;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.media.port.MediaStoragePort;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3-compatible {@link MediaStoragePort} (AWS S3 or MinIO via endpoint override).
 * Configure bucket lifecycle to ~7 years (2555 days) outside the app.
 */
public class S3MediaStorageAdapter implements MediaStoragePort {

    private final S3Client s3;
    private final String bucket;

    public S3MediaStorageAdapter(MediaProperties properties) {
        if (properties.getBucket() == null || properties.getBucket().isBlank()) {
            throw new IllegalStateException("vycepay.media.bucket is required when type=s3");
        }
        this.bucket = properties.getBucket();
        S3ClientBuilder builder = S3Client.builder().region(Region.of(properties.getRegion()));
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
            if (properties.isPathStyleAccess()) {
                builder.forcePathStyle(true);
            }
        }
        if (properties.getAccessKey() != null && !properties.getAccessKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        this.s3 = builder.build();
    }

    @Override
    public String store(String keyPrefix, String contentType, byte[] bytes, String fileHint) {
        String key = sanitize(keyPrefix) + "/" + UUID.randomUUID() + "-"
                + sanitizeFile(fileHint != null ? fileHint : "bin");
        try {
            PutObjectRequest.Builder put = PutObjectRequest.builder().bucket(bucket).key(key);
            if (contentType != null && !contentType.isBlank()) {
                put.contentType(contentType);
            }
            s3.putObject(put.build(), RequestBody.fromBytes(bytes != null ? bytes : new byte[0]));
            return key;
        } catch (S3Exception e) {
            throw new BusinessException("MEDIA_STORE_FAILED", "Failed to store media", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public byte[] load(String objectKey) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(sanitize(objectKey)).build())
                    .asByteArray();
        } catch (NoSuchKeyException e) {
            throw new BusinessException("MEDIA_NOT_FOUND", "Media not found", HttpStatus.NOT_FOUND);
        } catch (S3Exception e) {
            throw new BusinessException("MEDIA_LOAD_FAILED", "Failed to load media", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(sanitize(objectKey)).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "misc";
        }
        return raw.replace("..", "").replace('\\', '/').replaceAll("^/+", "");
    }

    private static String sanitizeFile(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
