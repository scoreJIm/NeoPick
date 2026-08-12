package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.media.PresignRequest;
import com.neopick.adapter.web.dto.media.PresignResponse;
import com.neopick.adapter.web.dto.media.UploadCompleteRequest;
import com.neopick.application.service.FileValidationService;
import com.neopick.infrastructure.config.NeopickProperties;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import com.neopick.port.security.SecurityContext;
import com.neopick.port.storage.FileStorage;
import com.neopick.port.storage.PresignedUrlResult;
import com.neopick.infrastructure.ratelimit.RateLimit;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media", description = "File upload, download, and deletion via presigned URLs")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final FileStorage fileStorage;
    private final FileValidationService fileValidationService;
    private final SecurityContext securityContext;
    private final BusinessMetrics businessMetrics;
    private final NeopickProperties properties;

    public MediaController(FileStorage fileStorage,
                           FileValidationService fileValidationService,
                           SecurityContext securityContext,
                           BusinessMetrics businessMetrics,
                           NeopickProperties properties) {
        this.fileStorage = fileStorage;
        this.fileValidationService = fileValidationService;
        this.securityContext = securityContext;
        this.businessMetrics = businessMetrics;
        this.properties = properties;
    }

    @PostMapping("/presign")
    @Timed(value = "neopick.media.presign", description = "Generate presigned upload URL")
    @Operation(summary = "Generate presigned upload URL", description = "Generates a time-limited presigned URL for direct file upload to cloud storage. Validates content type and file size before issuing the URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Presigned URL generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid content type or file exceeds size limit", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Upload rate limit exceeded", content = @Content)
    })
    public ApiResponse<PresignResponse> presign(@Valid @RequestBody PresignRequest request) {
        String userId = securityContext.requireCurrentUserId();

        fileValidationService.validateContentType(request.contentType());
        fileValidationService.validateFileSize(request.fileSize());

        String fileKey = fileValidationService.generateFileKey(
                request.type(), userId, request.contentType());

        Duration expiration = Duration.ofSeconds(properties.s3().presignUploadDuration());
        PresignedUrlResult result = fileStorage.generatePresignedUploadUrl(
                fileKey, request.contentType(), expiration);

        String cdnUrl = fileValidationService.buildCdnUrl(fileKey);

        businessMetrics.mediaUploaded();
        businessMetrics.mediaUploadBytes(request.fileSize());

        log.info("Presigned upload URL generated for user={}, key={}, size={}",
                userId, fileKey, request.fileSize());

        PresignResponse response = new PresignResponse(
                result.uploadUrl(), result.fileKey(), cdnUrl, result.expiresAt());
        return ApiResponse.success(response);
    }

    @GetMapping("/{fileKey}")
    @Timed(value = "neopick.media.download", description = "Redirect to presigned download URL")
    @Operation(summary = "Download a file by key", description = "Redirects to a time-limited presigned download URL for the specified file key. The client is redirected via HTTP 302.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "Redirect to presigned download URL"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    public ResponseEntity<Void> download(
            @Parameter(description = "File key returned from the presign/upload flow") @PathVariable String fileKey) {
        Duration expiration = Duration.ofSeconds(properties.s3().presignDownloadDuration());
        String downloadUrl = fileStorage.generatePresignedDownloadUrl(fileKey, expiration);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(downloadUrl))
                .build();
    }

    @DeleteMapping("/{fileKey}")
    @Timed(value = "neopick.media.delete", description = "Delete media file")
    @Operation(summary = "Delete a file", description = "Deletes a previously uploaded file from cloud storage by its file key.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    public ApiResponse<Void> delete(
            @Parameter(description = "File key to delete") @PathVariable String fileKey) {
        securityContext.requireCurrentUserId();
        fileStorage.deleteFile(fileKey);
        log.info("Media file deleted: {}", fileKey);
        return ApiResponse.success();
    }

    @PostMapping("/upload-complete")
    @Timed(value = "neopick.media.upload_complete", description = "Confirm upload complete")
    @Operation(summary = "Confirm upload completed", description = "Notifies the server that a file upload via presigned URL has completed successfully. Used for tracking and post-processing.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Upload completion acknowledged"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file key", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<Void> uploadComplete(@Valid @RequestBody UploadCompleteRequest request) {
        log.info("Upload complete callback received for fileKey={}", request.fileKey());
        return ApiResponse.success();
    }
}
