package com.neopick.adapter.web.dto.message;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(@NotBlank String content) {}
