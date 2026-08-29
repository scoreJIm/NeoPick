package com.neopick.adapter.web.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Confirmation that a file upload via presigned URL has completed")
public record UploadCompleteRequest(
        @NotBlank
        @Schema(description = "File key that was uploaded", example = "avatars/user123/2024/06/abc123.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
        String fileKey,

        @Schema(description = "Optional metadata about the upload", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String metadata
) {}
