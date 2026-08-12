package com.neopick.adapter.web.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Presigned URL response for file upload")
public record PresignResponse(
        @Schema(description = "Time-limited presigned upload URL", example = "https://s3.amazonaws.com/neopick-media/uploads/abc123.jpg?X-Amz-Expires=300...")
        String uploadUrl,

        @Schema(description = "Generated file key for storage", example = "avatars/user123/2024/06/abc123.jpg")
        String fileKey,

        @Schema(description = "CDN URL for the uploaded file after upload completes", example = "https://cdn.neopick.com/avatars/user123/2024/06/abc123.jpg")
        String cdnUrl,

        @Schema(description = "Expiration time of the presigned URL", example = "2024-06-14T10:05:00Z")
        Instant expiresAt
) {}
