package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByBookingId(String bookingId);

    List<PaymentJpaEntity> findByStatusAndCreatedAtBefore(String status, LocalDateTime before);

    List<PaymentJpaEntity> findByStatus(String status);
}
