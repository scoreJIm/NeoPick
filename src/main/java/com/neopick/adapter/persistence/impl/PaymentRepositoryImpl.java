package com.neopick.adapter.persistence.impl;

import com.neopick.adapter.persistence.entity.PaymentJpaEntity;
import com.neopick.adapter.persistence.repository.PaymentJpaRepository;
import com.neopick.domain.payment.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryImpl(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = toEntity(payment);
        PaymentJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByBookingId(String bookingId) {
        return jpaRepository.findByBookingId(bookingId).map(this::toDomain);
    }

    private PaymentJpaEntity toEntity(Payment p) {
        PaymentJpaEntity e = new PaymentJpaEntity();
        e.setId(p.getId().value());
        e.setBookingId(p.getBookingId());
        e.setAmount(p.getAmount());
        e.setMethod(p.getMethod().name());
        e.setStatus(p.getStatus().name());
        e.setTransactionId(p.getTransactionId());
        e.setPaidAt(p.getPaidAt());
        e.setRefundedAt(p.getRefundedAt());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        return e;
    }

    private Payment toDomain(PaymentJpaEntity e) {
        Payment p = new Payment(
                new PaymentId(e.getId()), e.getBookingId(), e.getAmount(),
                PaymentMethod.valueOf(e.getMethod()));
        return p;
    }
}
