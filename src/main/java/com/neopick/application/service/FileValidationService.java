package com.neopick.application.service;

import com.neopick.domain.media.FileTooLargeException;
import com.neopick.domain.media.MediaType;
import com.neopick.domain.media.UnsupportedFileTypeException;
import com.neopick.infrastructure.config.NeopickProperties;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class FileValidationService {

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] WEBP_MAGIC_START = {0x52, 0x49, 0x46, 0x46}; // RIFF
    private static final byte[] WEBP_MAGIC_END = {0x57, 0x45, 0x42, 0x50};   // WEBP

    private static final String JPEG_MIME = "image/jpeg";
    private static final String PNG_MIME = "image/png";
    private static final String WEBP_MIME = "image/webp";

    private final long maxFileSize;
    private final Set<String> allowedContentTypes;
    private final String cdnDomain;

    public FileValidationService(NeopickProperties properties) {
        this.maxFileSize = properties.media().maxFileSize();
        this.allowedContentTypes = Set.copyOf(properties.media().allowedTypes());
        this.cdnDomain = properties.cdn().domain();
    }

    /**
     * Validates that the content type is among allowed types.
     */
    public void validateContentType(String contentType) {
        if (!allowedContentTypes.contains(contentType)) {
            throw new UnsupportedFileTypeException(contentType);
        }
    }

    /**
     * Validates that the file size does not exceed the maximum allowed size.
     */
    public void validateFileSize(long fileSize) {
        if (fileSize > maxFileSize) {
            throw new FileTooLargeException(fileSize, maxFileSize);
        }
    }

    /**
     * Validates file magic bytes against its declared content type.
     * JPEG: FF D8 FF, PNG: 89 50 4E 47, WebP: 52 49 46 46 ... 57 45 42 50
     */
    public void validateMagicBytes(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length < 4) {
            throw new UnsupportedFileTypeException(contentType != null ? contentType : "unknown");
        }
        String inferredType = inferTypeFromMagicBytes(bytes);
        if (inferredType == null || !inferredType.equals(contentType)) {
            throw new UnsupportedFileTypeException(contentType);
        }
    }

    /**
     * Infers MIME type from magic bytes. Returns null if unrecognized.
     */
    public String inferTypeFromMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }
        if (startsWith(bytes, JPEG_MAGIC)) {
            return JPEG_MIME;
        }
        if (startsWith(bytes, PNG_MAGIC)) {
            return PNG_MIME;
        }
        if (startsWith(bytes, WEBP_MAGIC_START)
                && bytes.length >= 12
                && matchesAt(bytes, 8, WEBP_MAGIC_END)) {
            return WEBP_MIME;
        }
        return null;
    }

    /**
     * Generates a file key in the format: {type}/{userId}/{uuid}.{ext}
     */
    public String generateFileKey(MediaType type, String userId, String contentType) {
        String ext = mapContentTypeToExtension(contentType);
        return type.name().toLowerCase() + "/" + userId + "/" + UUID.randomUUID() + "." + ext;
    }

    /**
     * Builds the CDN URL for a given file key.
     */
    public String buildCdnUrl(String fileKey) {
        String domain = cdnDomain.endsWith("/") ? cdnDomain : cdnDomain + "/";
        return domain + fileKey;
    }

    private String mapContentTypeToExtension(String contentType) {
        return switch (contentType) {
            case JPEG_MIME -> "jpg";
            case PNG_MIME -> "png";
            case WEBP_MIME -> "webp";
            default -> throw new UnsupportedFileTypeException(contentType);
        };
    }

    private boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAt(byte[] data, int offset, byte[] magic) {
        if (data.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
