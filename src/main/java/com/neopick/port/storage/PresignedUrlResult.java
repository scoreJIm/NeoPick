package com.neopick.port.storage;

import java.time.Instant;

public record PresignedUrlResult(
        String uploadUrl,
        String fileKey,
        Instant expiresAt
) {}
