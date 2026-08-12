package com.neopick.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final Counter bookingsSubmitted;
    private final Counter bookingsConfirmed;
    private final Counter bookingsCancelled;
    private final Counter bookingsCompleted;
    private final Counter paymentsInitiated;
    private final Counter paymentsCompleted;
    private final Counter paymentsFailed;
    private final Counter paymentsRefunded;
    private final Counter usersRegistered;
    private final Counter reviewsSubmitted;
    private final Counter mediaUploaded;
    private final Counter mediaUploadBytes;
    private final Timer bookingSubmitTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.bookingsSubmitted = Counter.builder("neopick.bookings.submitted")
                .description("Total booking submissions").register(registry);
        this.bookingsConfirmed = Counter.builder("neopick.bookings.confirmed")
                .description("Total bookings confirmed").register(registry);
        this.bookingsCancelled = Counter.builder("neopick.bookings.cancelled")
                .description("Total bookings cancelled").register(registry);
        this.bookingsCompleted = Counter.builder("neopick.bookings.completed")
                .description("Total bookings completed").register(registry);
        this.paymentsInitiated = Counter.builder("neopick.payment.initiated")
                .description("Total payments initiated").register(registry);
        this.paymentsCompleted = Counter.builder("neopick.payment.completed")
                .description("Total payments completed").register(registry);
        this.paymentsFailed = Counter.builder("neopick.payment.failed")
                .description("Total payments failed").register(registry);
        this.paymentsRefunded = Counter.builder("neopick.payment.refunded")
                .description("Total payments refunded").register(registry);
        this.usersRegistered = Counter.builder("neopick.users.registered")
                .description("Total new user registrations").register(registry);
        this.reviewsSubmitted = Counter.builder("neopick.reviews.submitted")
                .description("Total reviews submitted").register(registry);
        this.mediaUploaded = Counter.builder("neopick.media.uploaded")
                .description("Total media presign requests").register(registry);
        this.mediaUploadBytes = Counter.builder("neopick.media.upload.bytes")
                .description("Total media upload size in bytes").register(registry);
        this.bookingSubmitTimer = Timer.builder("neopick.bookings.submit.duration")
                .description("Booking submit duration").register(registry);
    }

    public void bookingSubmitted() { bookingsSubmitted.increment(); }
    public void bookingConfirmed() { bookingsConfirmed.increment(); }
    public void bookingCancelled() { bookingsCancelled.increment(); }
    public void bookingCompleted() { bookingsCompleted.increment(); }
    public void paymentInitiated() { paymentsInitiated.increment(); }
    public void paymentCompleted() { paymentsCompleted.increment(); }
    public void paymentFailed() { paymentsFailed.increment(); }
    public void paymentRefunded() { paymentsRefunded.increment(); }
    public void userRegistered() { usersRegistered.increment(); }
    public void reviewSubmitted() { reviewsSubmitted.increment(); }
    public void mediaUploaded() { mediaUploaded.increment(); }
    public void mediaUploadBytes(long bytes) { mediaUploadBytes.increment(bytes); }
    public Timer bookingSubmitTimer() { return bookingSubmitTimer; }
}
