package com.neopick.adapter.web.dto.common;

import java.time.LocalDateTime;

public record ErrorResponse(
        int code,
        String message,
        String errorCode,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int code, String message, String errorCode) {
        return new ErrorResponse(code, message, errorCode, LocalDateTime.now());
    }
}
