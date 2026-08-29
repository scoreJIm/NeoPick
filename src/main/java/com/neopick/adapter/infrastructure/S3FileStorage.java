package com.neopick.adapter.infrastructure;

import com.neopick.infrastructure.config.NeopickProperties;
import com.neopick.port.storage.FileStorage;
import com.neopick.port.storage.PresignedUrlResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

@Component
public class S3FileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorage.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3FileStorage(S3Client s3Client,
                         S3Presigner s3Presigner,
                         NeopickProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = properties.aws().s3().bucket();
    }

    @Override
    @Retry(name = "s3Storage")
    @CircuitBreaker(name = "s3Storage", fallbackMethod = "uploadFallback")
    public String upload(String key, InputStream inputStream, String contentType, long contentLength) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
        log.info("Uploaded file to s3://{}/{}", bucket, key);
        return key;
    }

    @SuppressWarnings("unused")
    String uploadFallback(String key, InputStream inputStream, String contentType,
                          long contentLength, Throwable t) {
        log.error("S3 upload failed for key {} — circuit breaker open", key, t);
        throw new RuntimeException("S3 storage is temporarily unavailable. Please try again later.", t);
    }

    @Override
    public void delete(String key) {
        deleteFile(key);
    }

    @Override
    public String generatePresignedUrl(String key, long expirationSeconds) {
        return generatePresignedDownloadUrl(key, Duration.ofSeconds(expirationSeconds));
    }

    @Override
    public PresignedUrlResult generatePresignedUploadUrl(String fileKey, String contentType,
                                                          Duration expiration) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presigned.url().toString();
        Instant expiresAt = Instant.now().plus(expiration);
        log.info("Generated presigned upload URL for s3://{}/{}, expires at {}", bucket, fileKey, expiresAt);
        return new PresignedUrlResult(uploadUrl, fileKey, expiresAt);
    }

    @Override
    public String generatePresignedDownloadUrl(String fileKey, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        String downloadUrl = presigned.url().toString();
        log.debug("Generated presigned download URL for s3://{}/{}, expires in {}", bucket, fileKey, expiration);
        return downloadUrl;
    }

    @Override
    public void deleteFile(String fileKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();
        s3Client.deleteObject(deleteRequest);
        log.info("Deleted file s3://{}/{}", bucket, fileKey);
    }
}
