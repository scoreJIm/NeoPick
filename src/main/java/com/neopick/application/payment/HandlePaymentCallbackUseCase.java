package com.neopick.application.payment;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.notification.Notification;
import com.neopick.domain.notification.NotificationId;
import com.neopick.domain.notification.NotificationRepository;
import com.neopick.domain.notification.NotificationType;
import com.neopick.domain.payment.*;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import com.neopick.port.payment.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Orchestrates the payment callback flow: verify signature, update payment and booking
 * status, and create notifications for both parties.
 */
@Service
public class HandlePaymentCallbackUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandlePaymentCallbackUseCase.class);

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final BusinessMetrics metrics;

    public HandlePaymentCallbackUseCase(PaymentGateway paymentGateway,
                                         PaymentRepository paymentRepository,
                                         BookingRepository bookingRepository,
                                         NotificationRepository notificationRepository,
                                         BusinessMetrics metrics) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.metrics = metrics;
    }

    @Transactional
    public CallbackResult execute(Map<String, String> params) {
        PaymentGateway.CallbackResult verified = paymentGateway.verifyCallback(params);

        if (!verified.signatureValid()) {
            log.warn("Payment callback rejected: invalid signature");
            metrics.paymentFailed();
            return new CallbackResult(false, null);
        }

        if (!verified.success()) {
            log.warn("Payment callback with non-success status: {}", verified.tradeStatus());
            metrics.paymentFailed();
            return new CallbackResult(false, verified.outTradeNo());
        }

        String outTradeNo = verified.outTradeNo();
        PaymentId paymentId = PaymentId.from(outTradeNo);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + outTradeNo));

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Payment {} already marked as paid, ignoring duplicate callback", outTradeNo);
            return new CallbackResult(true, outTradeNo);
        }

        payment.markPaid(verified.tradeNo());
        paymentRepository.save(payment);

        Booking booking = bookingRepository.findById(BookingId.from(payment.getBookingId()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + payment.getBookingId()));
        booking.pay();
        bookingRepository.save(booking);

        createNotifications(payment, booking);

        metrics.paymentCompleted();
        log.info("Payment callback processed: paymentId={}, bookingId={}, tradeNo={}",
                outTradeNo, booking.getId().value(), verified.tradeNo());

        return new CallbackResult(true, outTradeNo);
    }

    private void createNotifications(Payment payment, Booking booking) {
        String amount = payment.getAmount().toString();
        String bookingId = booking.getId().value().toString();

        Notification studentNotification = new Notification(
                NotificationId.generate(),
                booking.getStudentId(),
                "Payment confirmed",
                "Your payment of " + amount + " CNY for booking " + bookingId + " has been confirmed.",
                NotificationType.PAYMENT,
                payment.getId().value().toString()
        );
        notificationRepository.save(studentNotification);

        Notification teacherNotification = new Notification(
                NotificationId.generate(),
                booking.getTeacherId().toString(),
                "Booking paid",
                "Booking " + bookingId + " has been paid (" + amount + " CNY).",
                NotificationType.PAYMENT,
                payment.getId().value().toString()
        );
        notificationRepository.save(teacherNotification);
    }

    public record CallbackResult(boolean success, String outTradeNo) {}
}
