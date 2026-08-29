package com.neopick.adapter.scheduled;

import com.neopick.adapter.persistence.entity.PaymentJpaEntity;
import com.neopick.adapter.persistence.repository.PaymentJpaRepository;
import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.payment.PaymentId;
import com.neopick.domain.payment.PaymentRepository;
import com.neopick.domain.payment.PaymentStatus;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);
    private static final int PAYMENT_EXPIRY_HOURS = 2;

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BusinessMetrics metrics;

    public BookingExpiryScheduler(PaymentJpaRepository paymentJpaRepository,
                                   PaymentRepository paymentRepository,
                                   BookingRepository bookingRepository,
                                   BusinessMetrics metrics) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.metrics = metrics;
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void expirePendingBookings() {
        log.debug("Checking for expired bookings and payments...");
        expireUnpaidPayments();
    }

    private void expireUnpaidPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(PAYMENT_EXPIRY_HOURS);
        List<PaymentJpaEntity> expiredPayments = paymentJpaRepository
                .findByStatusAndCreatedAtBefore(PaymentStatus.PENDING.name(), cutoff);

        for (PaymentJpaEntity entity : expiredPayments) {
            try {
                var payment = paymentRepository.findById(new PaymentId(entity.getId()));
                if (payment.isEmpty() || payment.get().getStatus() != PaymentStatus.PENDING) {
                    continue;
                }

                log.info("Cancelling expired payment: paymentId={}, bookingId={}",
                        entity.getId(), entity.getBookingId());

                var bookingOpt = bookingRepository.findById(BookingId.from(entity.getBookingId()));
                if (bookingOpt.isPresent()) {
                    Booking booking = bookingOpt.get();
                    booking.cancel("Payment expired after " + PAYMENT_EXPIRY_HOURS + " hours", "SYSTEM");
                    bookingRepository.save(booking);
                }

                metrics.paymentFailed();
            } catch (Exception e) {
                log.error("Failed to cancel expired payment {}: {}", entity.getId(), e.getMessage(), e);
            }
        }

        if (!expiredPayments.isEmpty()) {
            log.info("Cancelled {} expired payments", expiredPayments.size());
        }
    }
}
