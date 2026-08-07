package com.neopick.domain.message;

import com.neopick.domain.common.ValueObject;

import java.util.UUID;

public record ConversationId(UUID value) implements ValueObject {

    public static ConversationId generate() {
        return new ConversationId(UUID.randomUUID());
    }

    public static ConversationId from(String value) {
        return new ConversationId(UUID.fromString(value));
    }
}
