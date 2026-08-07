package com.neopick.adapter.web.dto.message;

import jakarta.validation.constraints.NotNull;

public record StartConversationRequest(@NotNull Long teacherId) {}
