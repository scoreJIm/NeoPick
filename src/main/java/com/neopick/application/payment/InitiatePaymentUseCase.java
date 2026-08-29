package com.neopick.application.payment;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.payment.*;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import com.neopick.port.payment.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InitiatePaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(InitiatePaymentUseCase.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BusinessMetrics metrics;
    private final List<PaymentGateway> gateways;

    public InitiatePaymentUseCase(PaymentRepository paymentRepository,
                                  BookingRepository bookingRepository,
                                  BusinessMetrics metrics,
                                  List<PaymentGateway> gateways) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.metrics = metrics;
        this.gateways = gateways;
    }

    @Transactional
    public PaymentResult execute(InitiatePaymentCommand command) {
        Booking booking = bookingRepository.findById(BookingId.from(command.bookingId()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        Payment payment = new Payment(PaymentId.generate(), command.bookingId(),
                booking.getPrice(), PaymentMethod.valueOf(command.method()));
        payment = paymentRepository.save(payment);

        PaymentGateway gateway = resolveGateway(command.method());
        PaymentGateway.InitiatePaymentResult gatewayResult = gateway.initiatePayment(payment, booking);

        metrics.paymentInitiated();
        log.info("Payment initiated: paymentId={}, bookingId={}, method={}, tradeNo={}",
                payment.getId().value(), command.bookingId(), command.method(),
                gatewayResult.tradeNo());

        return new PaymentResult(payment, gatewayResult.payUrl());
    }

    private PaymentGateway resolveGateway(String method) {
        return gateways.stream()
                .filter(g -> g.supportedMethod().equalsIgnoreCase(method))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment gateway found for method: " + method));
    }

    public record InitiatePaymentCommand(String bookingId, String method) {}

    public record PaymentResult(Payment payment, String payUrl) {}
}
