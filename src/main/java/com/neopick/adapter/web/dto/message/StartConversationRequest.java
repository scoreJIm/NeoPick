package com.neopick.adapter.web.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to start a new conversation with a teacher")
public record StartConversationRequest(
        @NotNull
        @Schema(description = "ID of the teacher to start a conversation with", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        Long teacherId
) {}
