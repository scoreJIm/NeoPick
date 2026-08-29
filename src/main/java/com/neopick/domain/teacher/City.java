package com.neopick.domain.teacher;

import com.neopick.domain.common.ValueObject;

public record City(String code, String name) implements ValueObject {

    public City {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("City code must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("City name must not be blank");
        }
    }
}
