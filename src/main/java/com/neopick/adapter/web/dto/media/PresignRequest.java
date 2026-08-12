package com.neopick.adapter.web.dto.media;

import com.neopick.domain.media.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to generate a presigned URL for direct file upload")
public record PresignRequest(
        @NotNull
        @Schema(description = "Media type category (AVATAR, COVER, LESSON_PHOTO)", example = "AVATAR", requiredMode = Schema.RequiredMode.REQUIRED)
        MediaType type,

        @NotBlank
        @Schema(description = "MIME content type of the file", example = "image/jpeg", requiredMode = Schema.RequiredMode.REQUIRED)
        String contentType,

        @NotNull @Positive
        @Schema(description = "File size in bytes", example = "1048576", requiredMode = Schema.RequiredMode.REQUIRED)
        Long fileSize
) {}
