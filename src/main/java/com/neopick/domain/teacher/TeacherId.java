package com.neopick.domain.teacher;

import com.neopick.domain.common.ValueObject;

public record TeacherId(Long value) implements ValueObject {

    public TeacherId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Teacher ID must be positive");
        }
    }
}
