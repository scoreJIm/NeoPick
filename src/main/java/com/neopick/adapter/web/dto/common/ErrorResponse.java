package com.neopick.adapter.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Error response with code, message, and timestamp")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "400") int code,
        @Schema(description = "Human-readable error message", example = "Invalid phone number format") String message,
        @Schema(description = "Application-specific error code", example = "INVALID_PHONE") String errorCode,
        @Schema(description = "Timestamp when the error occurred") LocalDateTime timestamp
) {
    public static ErrorResponse of(int code, String message, String errorCode) {
        return new ErrorResponse(code, message, errorCode, LocalDateTime.now());
    }
}
