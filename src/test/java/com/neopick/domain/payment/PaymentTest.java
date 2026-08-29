package com.neopick.domain.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @DisplayName("Transition: PENDING -> markPaid() -> PAID")
    class MarkPaid {

        @Test
        @DisplayName("should transition to PAID with transaction ID")
        void shouldTransitionToPaid() {
            payment.markPaid("txn-12345");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getTransactionId()).isEqualTo("txn-12345");
            assertThat(payment.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("should set paidAt timestamp")
        void shouldSetPaidAtTimestamp() {
            LocalDateTime before = LocalDateTime.now();
            payment.markPaid("txn-67890");
            assertThat(payment.getPaidAt()).isNotNull();
            assertThat(payment.getPaidAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("should support multiple markPaid calls (idempotent from domain perspective)")
        void shouldSupportMultipleMarkPaid() {
            payment.markPaid("txn-1");
            payment.markPaid("txn-2");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getTransactionId()).isEqualTo("txn-2");
        }
    }

    @Nested
    @DisplayName("Transition: PAID -> markRefunded() -> REFUNDED")
    class MarkRefunded {

        @Test
        @DisplayName("should transition to REFUNDED")
        void shouldTransitionToRefunded() {
            payment.markPaid("txn-123");
            payment.markRefunded();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getRefundedAt()).isNotNull();
        }

        @Test
        @DisplayName("should set refundedAt timestamp")
        void shouldSetRefundedAtTimestamp() {
            payment.markPaid("txn-123");
            LocalDateTime before = LocalDateTime.now();
            payment.markRefunded();
            assertThat(payment.getRefundedAt()).isAfterOrEqualTo(before);
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

    @Nested
    @DisplayName("Alipay callback scenarios")
    class AlipayCallbackScenarios {

        @Test
        @DisplayName("should handle successful Alipay callback with correct transaction ID")
        void shouldHandleSuccessfulAlipayCallback() {
            payment.markPaid("alipay_txn_20240101_001");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getTransactionId()).isEqualTo("alipay_txn_20240101_001");
            assertThat(payment.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("should not change state if already PAID and callback received again")
        void shouldBeIdempotentOnDuplicateCallback() {
            payment.markPaid("first_txn");
            LocalDateTime firstPaidAt = payment.getPaidAt();

            payment.markPaid("second_txn");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getTransactionId()).isEqualTo("second_txn");
            assertThat(payment.getPaidAt()).isAfterOrEqualTo(firstPaidAt);
        }
    }

    @Nested
    @DisplayName("Payment expiry scenarios")
    class PaymentExpiryScenarios {

        @Test
        @DisplayName("PENDING payment created over 2 hours ago should be eligible for cancellation")
        void shouldBeEligibleForCancellationWhenExpired() {
            Payment expired = Payment.reconstruct(
                    PaymentId.generate(), "booking-expired", new BigDecimal("200.00"),
                    PaymentMethod.ALIPAY, PaymentStatus.PENDING, null, null, null,
                    LocalDateTime.now().minusHours(3), LocalDateTime.now());
            assertThat(expired.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(expired.getCreatedAt()).isBefore(LocalDateTime.now().minusHours(2));
        }

        @Test
        @DisplayName("already PAID payment should not be cancelled by expiry scheduler")
        void shouldNotCancelAlreadyPaidPayment() {
            payment.markPaid("txn-paid");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        }
    }

    @Nested
    @DisplayName("Reconstruct")
    class Reconstruct {

        @Test
        @DisplayName("should reconstruct payment with all fields")
        void shouldReconstructPaymentWithAllFields() {
            LocalDateTime now = LocalDateTime.now();
            Payment reconstructed = Payment.reconstruct(
                    PaymentId.generate(), "booking-recon", new BigDecimal("500.00"),
                    PaymentMethod.ALIPAY, PaymentStatus.REFUNDED, "txn-recon",
                    now.minusDays(1), now, now.minusDays(2), now);

            assertThat(reconstructed.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(reconstructed.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(reconstructed.getTransactionId()).isEqualTo("txn-recon");
            assertThat(reconstructed.getPaidAt()).isEqualTo(now.minusDays(1));
            assertThat(reconstructed.getRefundedAt()).isEqualTo(now);
        }
    }
}
