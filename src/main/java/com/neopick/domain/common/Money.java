package com.neopick.domain.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) implements ValueObject {

    private static final Currency CNY = Currency.getInstance("CNY");

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency must not be null");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money cny(BigDecimal amount) {
        return new Money(amount, CNY);
    }

    public static Money cny(double amount) {
        return new Money(BigDecimal.valueOf(amount), CNY);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, CNY);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on different currencies");
        }
    }
}
