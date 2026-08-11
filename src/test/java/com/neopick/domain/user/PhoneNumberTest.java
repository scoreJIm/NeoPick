package com.neopick.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PhoneNumber")
class PhoneNumberTest {

    @Nested
    @DisplayName("Valid phone numbers")
    class ValidPhoneNumbers {

        @ParameterizedTest
        @ValueSource(strings = {"13800138000", "15912345678", "18888888888", "13600001111"})
        @DisplayName("should accept valid 11-digit China mobile numbers")
        void shouldAcceptValidNumbers(String number) {
            PhoneNumber phone = PhoneNumber.of(number);
            assertThat(phone.value()).isEqualTo(number);
        }
    }

    @Nested
    @DisplayName("Invalid phone numbers")
    class InvalidPhoneNumbers {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "abc", "123", "12345678901", "23800138000", "1380013800"})
        @DisplayName("should reject invalid phone numbers")
        void shouldRejectInvalidNumbers(String invalid) {
            assertThatThrownBy(() -> PhoneNumber.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Masked phone")
    class Masked {

        @Test
        @DisplayName("should mask middle 4 digits")
        void shouldMaskPhoneNumber() {
            PhoneNumber phone = PhoneNumber.of("13800138000");
            assertThat(phone.masked()).isEqualTo("138****8000");
        }

        @Test
        @DisplayName("masked should not reveal full number")
        void maskedShouldNotRevealFullNumber() {
            PhoneNumber phone = PhoneNumber.of("15912345678");
            assertThat(phone.masked()).doesNotContain("1234");
            assertThat(phone.masked()).isEqualTo("159****5678");
        }
    }
}
