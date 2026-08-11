package com.neopick.application.payment;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.payment.*;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitiatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BusinessMetrics metrics;

    public InitiatePaymentUseCase(PaymentRepository paymentRepository,
                                  BookingRepository bookingRepository, BusinessMetrics metrics) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.metrics = metrics;
    }

    @Transactional
    public PaymentResult execute(InitiatePaymentCommand command) {
        Booking booking = bookingRepository.findById(BookingId.from(command.bookingId()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        Payment payment = new Payment(PaymentId.generate(), command.bookingId(),
                booking.getPrice(), PaymentMethod.valueOf(command.method()));
        payment = paymentRepository.save(payment);
        metrics.paymentInitiated();
        return new PaymentResult(payment, "https://pay.example.com/order/" + payment.getId().value());
    }

    public record InitiatePaymentCommand(String bookingId, String method) {}

    public record PaymentResult(Payment payment, String payUrl) {}
}
