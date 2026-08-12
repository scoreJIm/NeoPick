package com.neopick.domain.notification;

import com.neopick.domain.common.ValueObject;

import java.util.UUID;

public record NotificationId(UUID value) implements ValueObject {

    public static NotificationId generate() {
        return new NotificationId(UUID.randomUUID());
    }

    public static NotificationId from(String value) {
        return new NotificationId(UUID.fromString(value));
    }
}
