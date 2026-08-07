package com.neopick.domain.booking;

import com.neopick.domain.common.ValueObject;

public record Address(String label, String detail, double latitude, double longitude) implements ValueObject {

    public Address {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Address label must not be blank");
        }
        if (detail == null) {
            detail = "";
        }
    }
}
