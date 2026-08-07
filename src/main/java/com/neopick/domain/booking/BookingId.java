package com.neopick.domain.booking;

import com.neopick.domain.common.ValueObject;

import java.util.UUID;

public record BookingId(UUID value) implements ValueObject {

    public BookingId {
        if (value == null) {
            throw new IllegalArgumentException("Booking ID must not be null");
        }
    }

    public static BookingId generate() {
        return new BookingId(UUID.randomUUID());
    }

    public static BookingId from(String value) {
        return new BookingId(UUID.fromString(value));
    }
}
