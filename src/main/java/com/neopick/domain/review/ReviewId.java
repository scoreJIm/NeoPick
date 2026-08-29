package com.neopick.domain.review;

import com.neopick.domain.common.ValueObject;

import java.util.UUID;

public record ReviewId(UUID value) implements ValueObject {

    public static ReviewId generate() {
        return new ReviewId(UUID.randomUUID());
    }
}
