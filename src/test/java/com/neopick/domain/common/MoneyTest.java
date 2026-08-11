package com.neopick.domain.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create Money with valid amount")
        void shouldCreateMoney() {
            Money m = Money.cny(new BigDecimal("100.00"));
            assertThat(m.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should round to 2 decimal places")
        void shouldRoundToScale2() {
            Money m = Money.cny(new BigDecimal("100.456"));
            assertThat(m.amount()).isEqualByComparingTo(new BigDecimal("100.46"));
        }

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> Money.cny(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegative() {
            assertThatThrownBy(() -> Money.cny(new BigDecimal("-1.00")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should accept zero amount")
        void shouldAcceptZero() {
            Money m = Money.zero();
            assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should create from double via cny()")
        void shouldCreateFromDouble() {
            Money m = Money.cny(150.50);
            assertThat(m.amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        }
    }

    @Nested
    @DisplayName("Arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("should add two Money amounts")
        void shouldAdd() {
            Money a = Money.cny(100);
            Money b = Money.cny(50);
            assertThat(a.add(b).amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should subtract two Money amounts")
        void shouldSubtract() {
            Money a = Money.cny(100);
            Money b = Money.cny(30);
            assertThat(a.subtract(b).amount()).isEqualByComparingTo(new BigDecimal("70.00"));
        }

        @Test
        @DisplayName("isGreaterThan should return true when greater")
        void shouldCompareGreater() {
            assertThat(Money.cny(200).isGreaterThan(Money.cny(100))).isTrue();
            assertThat(Money.cny(100).isGreaterThan(Money.cny(200))).isFalse();
        }
    }
}
