package com.neopick.domain.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment State Machine")
class PaymentTest {

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = new Payment(PaymentId.generate(), "booking-1",
                new BigDecimal("300.00"), PaymentMethod.WECHAT);
    }

    @Nested
    @DisplayName("Initial state")
    class InitialState {

        @Test
        @DisplayName("new payment should be PENDING")
        void shouldBePending() {
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getBookingId()).isEqualTo("booking-1");
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(payment.getMethod()).isEqualTo(PaymentMethod.WECHAT);
            assertThat(payment.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Transition: PENDING → markPaid() → PAID")
    class MarkPaid {

        @Test
        @DisplayName("should transition to PAID with transaction ID")
        void shouldTransitionToPaid() {
            payment.markPaid("txn-12345");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getTransactionId()).isEqualTo("txn-12345");
            assertThat(payment.getPaidAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Transition: PAID → markRefunded() → REFUNDED")
    class MarkRefunded {

        @Test
        @DisplayName("should transition to REFUNDED")
        void shouldTransitionToRefunded() {
            payment.markPaid("txn-123");
            payment.markRefunded();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getRefundedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("PaymentMethod enum")
    class PaymentMethodValues {

        @Test
        @DisplayName("should support WECHAT and ALIPAY")
        void shouldSupportMethods() {
            assertThat(PaymentMethod.valueOf("WECHAT")).isEqualTo(PaymentMethod.WECHAT);
            assertThat(PaymentMethod.valueOf("ALIPAY")).isEqualTo(PaymentMethod.ALIPAY);
        }
    }
}
