package com.neopick.adapter.persistence.impl;

import com.neopick.adapter.persistence.entity.BookingJpaEntity;
import com.neopick.adapter.persistence.repository.BookingJpaRepository;
import com.neopick.domain.booking.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BookingRepositoryImpl implements BookingRepository {

    private final BookingJpaRepository jpaRepository;

    public BookingRepositoryImpl(BookingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Booking save(Booking booking) {
        BookingJpaEntity entity = toEntity(booking);
        BookingJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(BookingId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Booking> findByStudentId(String studentId, BookingStatus status, int page, int size) {
        String statusStr = status != null ? status.name() : null;
        PageRequest pageRequest = PageRequest.of(page, size);
        return jpaRepository.findByStudentIdAndStatus(studentId, statusStr, pageRequest)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Booking> findByTeacherId(Long teacherId, BookingStatus status, int page, int size) {
        String statusStr = status != null ? status.name() : null;
        PageRequest pageRequest = PageRequest.of(page, size);
        return jpaRepository.findByTeacherIdAndStatus(teacherId, statusStr, pageRequest)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByStudentId(String studentId, BookingStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.countByStudentIdAndStatus(studentId, statusStr);
    }

    @Override
    public long countByTeacherId(Long teacherId, BookingStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.countByTeacherIdAndStatus(teacherId, statusStr);
    }

    private BookingJpaEntity toEntity(Booking b) {
        BookingJpaEntity e = new BookingJpaEntity();
        e.setId(b.getId().value());
        e.setStudentId(b.getStudentId());
        e.setTeacherId(b.getTeacherId());
        e.setStatus(b.getStatus().name());
        e.setScheduledStart(b.getScheduledStart());
        e.setScheduledEnd(b.getScheduledEnd());
        e.setDurationMinutes(b.getDurationMinutes());
        e.setPrice(b.getPrice());
        if (b.getAddress() != null) {
            e.setAddressLabel(b.getAddress().label());
            e.setAddressDetail(b.getAddress().detail());
            e.setAddressLat(BigDecimal.valueOf(b.getAddress().latitude()));
            e.setAddressLng(BigDecimal.valueOf(b.getAddress().longitude()));
        }
        e.setStudentNote(b.getStudentNote());
        e.setCancelReason(b.getCancelReason());
        e.setCancelledBy(b.getCancelledBy());
        e.setConfirmedAt(b.getConfirmedAt());
        e.setPaidAt(b.getPaidAt());
        e.setCompletedAt(b.getCompletedAt());
        e.setCancelledAt(b.getCancelledAt());
        e.setCreatedAt(b.getCreatedAt());
        e.setUpdatedAt(b.getUpdatedAt());
        return e;
    }

    private Booking toDomain(BookingJpaEntity e) {
        Booking b = new Booking(
                new BookingId(e.getId()), e.getStudentId(), e.getTeacherId(),
                e.getScheduledStart(), e.getScheduledEnd(), e.getDurationMinutes(),
                e.getPrice(),
                new Address(e.getAddressLabel(), e.getAddressDetail(),
                        e.getAddressLat() != null ? e.getAddressLat().doubleValue() : 0,
                        e.getAddressLng() != null ? e.getAddressLng().doubleValue() : 0),
                e.getStudentNote());
        return b;
    }
}
