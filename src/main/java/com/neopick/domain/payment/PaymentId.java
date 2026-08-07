package com.neopick.domain.payment;

import com.neopick.domain.common.ValueObject;

import java.util.UUID;

public record PaymentId(UUID value) implements ValueObject {

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public static PaymentId from(String value) {
        return new PaymentId(UUID.fromString(value));
    }
}
