package com.neopick.adapter.scheduled;

import com.neopick.adapter.persistence.entity.PaymentJpaEntity;
import com.neopick.adapter.persistence.repository.PaymentJpaRepository;
import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.payment.Payment;
import com.neopick.domain.payment.PaymentId;
import com.neopick.domain.payment.PaymentMethod;
import com.neopick.domain.payment.PaymentRepository;
import com.neopick.infrastructure.config.NeopickProperties;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("BookingExpiryScheduler")
class BookingExpirySchedulerTest {

    private PaymentJpaRepository paymentJpaRepository;
    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private BusinessMetrics metrics;
    private BookingExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        paymentJpaRepository = mock(PaymentJpaRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        metrics = new BusinessMetrics(new SimpleMeterRegistry());
        scheduler = new BookingExpiryScheduler(
                paymentJpaRepository, paymentRepository, bookingRepository, metrics);
    }

    @Test
    @DisplayName("should run without exceptions when no expired payments")
    void shouldRunWithoutExceptionsWhenNoExpiredPayments() {
        when(paymentJpaRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of());

        assertThatCode(scheduler::expirePendingBookings)
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should cancel expired payments and update bookings")
    void shouldCancelExpiredPaymentsAndUpdateBookings() {
        PaymentJpaEntity expired = new PaymentJpaEntity();
        expired.setId(java.util.UUID.randomUUID());
        expired.setBookingId("booking-123");
        expired.setStatus("PENDING");
        expired.setCreatedAt(LocalDateTime.now().minusHours(3));

        when(paymentJpaRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of(expired));

        Payment payment = new Payment(
                new PaymentId(expired.getId()), expired.getBookingId(),
                new BigDecimal("300.00"), PaymentMethod.ALIPAY);
        when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));

        Booking booking = mock(Booking.class);
        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

        scheduler.expirePendingBookings();

        verify(booking).cancel(any(), eq("SYSTEM"));
        verify(bookingRepository).save(booking);
    }
}
