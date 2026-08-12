package com.neopick.adapter.web.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to send a message in an existing conversation")
public record SendMessageRequest(
        @NotBlank
        @Schema(description = "Message content text", example = "Hi, I'd like to schedule a lesson for next week.", requiredMode = Schema.RequiredMode.REQUIRED)
        String content
) {}
