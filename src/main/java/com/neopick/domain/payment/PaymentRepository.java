package com.neopick.domain.payment;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId id);

    Optional<Payment> findByBookingId(String bookingId);
}
