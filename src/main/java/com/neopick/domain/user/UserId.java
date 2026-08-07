package com.neopick.domain.user;

import com.neopick.domain.common.ValueObject;

import java.util.UUID;

public record UserId(UUID value) implements ValueObject {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId from(String value) {
        return new UserId(UUID.fromString(value));
    }
}
