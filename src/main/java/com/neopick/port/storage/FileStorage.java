package com.neopick.port.storage;

import java.io.InputStream;
import java.time.Duration;

public interface FileStorage {

    String upload(String key, InputStream inputStream, String contentType, long contentLength);

    void delete(String key);

    String generatePresignedUrl(String key, long expirationSeconds);

    /**
     * Generates a presigned URL for direct file upload to S3.
     *
     * @param fileKey     the object key in S3
     * @param contentType the MIME type of the file to be uploaded
     * @param expiration  how long the presigned URL is valid
     * @return presigned upload URL result with URL, key, and expiry
     */
    PresignedUrlResult generatePresignedUploadUrl(String fileKey, String contentType, Duration expiration);

    /**
     * Generates a presigned URL for downloading/viewing a file.
     *
     * @param fileKey    the object key in S3
     * @param expiration how long the presigned URL is valid
     * @return presigned download URL as string
     */
    String generatePresignedDownloadUrl(String fileKey, Duration expiration);

    /**
     * Deletes a file from storage.
     *
     * @param fileKey the object key in S3
     */
    void deleteFile(String fileKey);
}
