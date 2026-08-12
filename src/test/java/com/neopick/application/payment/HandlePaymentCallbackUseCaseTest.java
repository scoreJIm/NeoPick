package com.neopick.application.payment;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.booking.BookingStatus;
import com.neopick.domain.notification.NotificationRepository;
import com.neopick.domain.payment.*;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import com.neopick.port.payment.PaymentGateway;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("HandlePaymentCallbackUseCase")
class HandlePaymentCallbackUseCaseTest {

    private PaymentGateway paymentGateway;
    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private NotificationRepository notificationRepository;
    private BusinessMetrics metrics;
    private HandlePaymentCallbackUseCase useCase;

    @BeforeEach
    void setUp() {
        paymentGateway = mock(PaymentGateway.class);
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        metrics = new BusinessMetrics(new SimpleMeterRegistry());
        useCase = new HandlePaymentCallbackUseCase(
                paymentGateway, paymentRepository, bookingRepository,
                notificationRepository, metrics);
    }

    @Nested
    @DisplayName("Successful callback")
    class SuccessfulCallback {

        @Test
        @DisplayName("should update payment and booking on successful callback")
        void shouldUpdatePaymentAndBookingOnSuccess() {
            UUID paymentUuid = UUID.randomUUID();
            UUID bookingUuid = UUID.randomUUID();
            String tradeNo = "alipay_txn_001";

            Map<String, String> params = Map.of(
                    "out_trade_no", paymentUuid.toString(),
                    "trade_no", tradeNo,
                    "total_amount", "300.00",
                    "trade_status", "TRADE_SUCCESS"
            );

            when(paymentGateway.verifyCallback(params))
                    .thenReturn(new PaymentGateway.CallbackResult(
                            true, true, tradeNo, paymentUuid.toString(),
                            new BigDecimal("300.00"), "TRADE_SUCCESS"));

            Payment payment = new Payment(new PaymentId(paymentUuid), bookingUuid.toString(),
                    new BigDecimal("300.00"), PaymentMethod.ALIPAY);
            when(paymentRepository.findById(new PaymentId(paymentUuid)))
                    .thenReturn(Optional.of(payment));

            Booking booking = Booking.reconstruct(
                    new BookingId(bookingUuid), "student-1", 100L,
                    BookingStatus.PENDING_PAY, null, null, 0,
                    new BigDecimal("300.00"), null, null, null, null,
                    null, null, null, null, null, null);

            when(bookingRepository.findById(new BookingId(bookingUuid)))
                    .thenReturn(Optional.of(booking));
            when(paymentRepository.save(any())).thenReturn(payment);
            when(bookingRepository.save(any())).thenReturn(booking);
            when(notificationRepository.save(any())).thenReturn(null);

            var result = useCase.execute(params);

            assertThat(result.success()).isTrue();
            assertThat(result.outTradeNo()).isEqualTo(paymentUuid.toString());
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_CLASS);
            verify(notificationRepository, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("Invalid signature")
    class InvalidSignature {

        @Test
        @DisplayName("should reject callback with invalid signature")
        void shouldRejectCallbackWithInvalidSignature() {
            Map<String, String> params = Map.of("out_trade_no", UUID.randomUUID().toString());

            when(paymentGateway.verifyCallback(params))
                    .thenReturn(new PaymentGateway.CallbackResult(
                            false, false, null, null, null, null));

            var result = useCase.execute(params);

            assertThat(result.success()).isFalse();
            verify(paymentRepository, never()).save(any());
            verify(bookingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Duplicate callback")
    class DuplicateCallback {

        @Test
        @DisplayName("should ignore duplicate callback when payment already PAID")
        void shouldIgnoreDuplicateCallback() {
            UUID paymentUuid = UUID.randomUUID();

            Map<String, String> params = Map.of(
                    "out_trade_no", paymentUuid.toString(),
                    "trade_no", "alipay_txn_002",
                    "total_amount", "300.00",
                    "trade_status", "TRADE_SUCCESS"
            );

            when(paymentGateway.verifyCallback(params))
                    .thenReturn(new PaymentGateway.CallbackResult(
                            true, true, "alipay_txn_002", paymentUuid.toString(),
                            new BigDecimal("300.00"), "TRADE_SUCCESS"));

            Payment alreadyPaid = Payment.reconstruct(
                    new PaymentId(paymentUuid), UUID.randomUUID().toString(),
                    new BigDecimal("300.00"), PaymentMethod.ALIPAY,
                    PaymentStatus.PAID, "alipay_txn_001", null, null,
                    null, null);

            when(paymentRepository.findById(new PaymentId(paymentUuid)))
                    .thenReturn(Optional.of(alreadyPaid));

            var result = useCase.execute(params);

            assertThat(result.success()).isTrue();
            verify(bookingRepository, never()).save(any());
            verify(notificationRepository, never()).save(any());
        }
    }
}
