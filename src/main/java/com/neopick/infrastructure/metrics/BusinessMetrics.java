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
    private final Counter usersRegistered;
    private final Counter reviewsSubmitted;
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
        this.paymentsInitiated = Counter.builder("neopick.payments.initiated")
                .description("Total payments initiated").register(registry);
        this.paymentsCompleted = Counter.builder("neopick.payments.completed")
                .description("Total payments completed").register(registry);
        this.usersRegistered = Counter.builder("neopick.users.registered")
                .description("Total new user registrations").register(registry);
        this.reviewsSubmitted = Counter.builder("neopick.reviews.submitted")
                .description("Total reviews submitted").register(registry);
        this.bookingSubmitTimer = Timer.builder("neopick.bookings.submit.duration")
                .description("Booking submit duration").register(registry);
    }

    public void bookingSubmitted() { bookingsSubmitted.increment(); }
    public void bookingConfirmed() { bookingsConfirmed.increment(); }
    public void bookingCancelled() { bookingsCancelled.increment(); }
    public void bookingCompleted() { bookingsCompleted.increment(); }
    public void paymentInitiated() { paymentsInitiated.increment(); }
    public void paymentCompleted() { paymentsCompleted.increment(); }
    public void userRegistered() { usersRegistered.increment(); }
    public void reviewSubmitted() { reviewsSubmitted.increment(); }
    public Timer bookingSubmitTimer() { return bookingSubmitTimer; }
}
