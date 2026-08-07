package com.neopick.domain.user;

import com.neopick.domain.common.ValueObject;
import com.neopick.shared.Constants;

import java.util.regex.Pattern;

public record PhoneNumber(String value) implements ValueObject {

    private static final Pattern PHONE_PATTERN = Pattern.compile(Constants.CHINA_PHONE_REGEX);

    public PhoneNumber {
        if (value == null || !PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + value);
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    public String masked() {
        return value.substring(0, 3) + "****" + value.substring(7);
    }
}
