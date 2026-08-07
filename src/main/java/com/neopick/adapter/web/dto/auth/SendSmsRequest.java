package com.neopick.adapter.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendSmsRequest(
        @NotBlank
        @Pattern(regexp = "^\\+86\\d{11}$", message = "Invalid phone number format")
        String phone
) {}
