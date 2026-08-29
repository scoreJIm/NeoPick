package com.neopick.domain.auth;

import com.neopick.domain.common.ValueObject;

public record TokenPair(String accessToken, String refreshToken) implements ValueObject {
}
