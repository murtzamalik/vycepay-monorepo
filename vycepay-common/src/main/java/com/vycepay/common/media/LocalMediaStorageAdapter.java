package com.vycepay.common.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.media.port.MediaStoragePort;

/**
 * Filesystem {@link MediaStoragePort} for local/dev (and prod when type=local).
 */
public class LocalMediaStorageAdapter implements MediaStoragePort {

    private final Path basePath;

    public LocalMediaStorageAdapter(String localBasePath) {
        this.basePath = Path.of(localBasePath).toAbsolutePath().normalize();
    }

    @Override
    public String store(String keyPrefix, String contentType, byte[] bytes, String fileHint) {
        try {
            String safePrefix = sanitize(keyPrefix);
            String name = UUID.randomUUID() + "-" + sanitizeFile(fileHint != null ? fileHint : "bin");
            Path dir = basePath.resolve(safePrefix);
            Files.createDirectories(dir);
            Path file = dir.resolve(name);
            Files.write(file, bytes != null ? bytes : new byte[0]);
            return safePrefix + "/" + name;
        } catch (IOException e) {
            throw new BusinessException("MEDIA_STORE_FAILED", "Failed to store media", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public byte[] load(String objectKey) {
        try {
            Path file = resolve(objectKey);
            if (!Files.isRegularFile(file)) {
                throw new BusinessException("MEDIA_NOT_FOUND", "Media not found", HttpStatus.NOT_FOUND);
            }
            return Files.readAllBytes(file);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("MEDIA_LOAD_FAILED", "Failed to load media", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            return Files.isRegularFile(resolve(objectKey));
        } catch (Exception e) {
            return false;
        }
    }

    private Path resolve(String objectKey) {
        Path resolved = basePath.resolve(sanitize(objectKey)).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new BusinessException("MEDIA_NOT_FOUND", "Invalid media key", HttpStatus.NOT_FOUND);
        }
        return resolved;
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
