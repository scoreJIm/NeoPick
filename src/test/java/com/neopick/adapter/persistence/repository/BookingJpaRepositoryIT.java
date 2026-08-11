package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.BookingJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("BookingJpaRepository Integration Tests")
class BookingJpaRepositoryIT {

    @Autowired private BookingJpaRepository repository;

    @Test
    @DisplayName("should save and find booking by id")
    void shouldSaveAndFindById() {
        BookingJpaEntity entity = createBooking(UUID.randomUUID(), "student-1",
                100L, "PENDING_CONFIRM");

        BookingJpaEntity saved = repository.save(entity);
        Optional<BookingJpaEntity> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStudentId()).isEqualTo("student-1");
        assertThat(found.get().getTeacherId()).isEqualTo(100L);
        assertThat(found.get().getStatus()).isEqualTo("PENDING_CONFIRM");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("should find bookings by student id and status")
    void shouldFindByStudentIdAndStatus() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        repository.save(createBooking(id1, "student-A", 100L, "PENDING_CONFIRM"));
        repository.save(createBooking(id2, "student-A", 200L, "PENDING_CONFIRM"));
        repository.save(createBooking(UUID.randomUUID(), "student-B", 100L, "COMPLETED"));

        List<BookingJpaEntity> results = repository.findByStudentIdAndStatus(
                "student-A", "PENDING_CONFIRM", PageRequest.of(0, 20));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(b -> b.getStudentId().equals("student-A"));
        assertThat(results).allMatch(b -> b.getStatus().equals("PENDING_CONFIRM"));
    }

    @Test
    @DisplayName("should return empty list when no matching bookings")
    void shouldReturnEmptyForNoMatches() {
        List<BookingJpaEntity> results = repository.findByStudentIdAndStatus(
                "nonexistent", "PENDING_CONFIRM", PageRequest.of(0, 20));

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("should count bookings by student and status")
    void shouldCountByStudentAndStatus() {
        repository.save(createBooking(UUID.randomUUID(), "student-C", 100L, "PENDING_PAY"));
        repository.save(createBooking(UUID.randomUUID(), "student-C", 200L, "PENDING_PAY"));
        repository.save(createBooking(UUID.randomUUID(), "student-C", 300L, "COMPLETED"));

        long pendingPay = repository.countByStudentIdAndStatus("student-C", "PENDING_PAY");
        long completed = repository.countByStudentIdAndStatus("student-C", "COMPLETED");

        assertThat(pendingPay).isEqualTo(2);
        assertThat(completed).isEqualTo(1);
    }

    @Test
    @DisplayName("should find bookings by teacher id")
    void shouldFindByTeacherId() {
        repository.save(createBooking(UUID.randomUUID(), "s1", 500L, "PENDING_CONFIRM"));
        repository.save(createBooking(UUID.randomUUID(), "s2", 500L, "PENDING_CONFIRM"));
        repository.save(createBooking(UUID.randomUUID(), "s3", 600L, "PENDING_CONFIRM"));

        List<BookingJpaEntity> results = repository.findByTeacherIdAndStatus(
                500L, "PENDING_CONFIRM", PageRequest.of(0, 20));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(b -> b.getTeacherId() == 500L);
    }

    @Test
    @DisplayName("should persist all booking fields correctly")
    void shouldPersistAllFields() {
        UUID id = UUID.randomUUID();
        BookingJpaEntity entity = new BookingJpaEntity();
        entity.setId(id);
        entity.setStudentId("student-full");
        entity.setTeacherId(999L);
        entity.setStatus("PENDING_CONFIRM");
        entity.setScheduledStart(LocalDateTime.of(2026, 8, 15, 14, 0));
        entity.setScheduledEnd(LocalDateTime.of(2026, 8, 15, 15, 0));
        entity.setDurationMinutes(60);
        entity.setPrice(new BigDecimal("450.00"));
        entity.setAddressLabel("Studio");
        entity.setAddressDetail("Room 2");
        entity.setAddressLat(new BigDecimal("31.2304000"));
        entity.setAddressLng(new BigDecimal("121.4737000"));
        entity.setStudentNote("Full field test");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        BookingJpaEntity saved = repository.save(entity);
        Optional<BookingJpaEntity> found = repository.findById(id);

        assertThat(found).isPresent();
        BookingJpaEntity f = found.get();
        assertThat(f.getDurationMinutes()).isEqualTo(60);
        assertThat(f.getAddressLabel()).isEqualTo("Studio");
        assertThat(f.getAddressLat()).isEqualByComparingTo(new BigDecimal("31.2304000"));
        assertThat(f.getAddressLng()).isEqualByComparingTo(new BigDecimal("121.4737000"));
        assertThat(f.getStudentNote()).isEqualTo("Full field test");
    }

    private BookingJpaEntity createBooking(UUID id, String studentId,
                                            Long teacherId, String status) {
        BookingJpaEntity entity = new BookingJpaEntity();
        entity.setId(id);
        entity.setStudentId(studentId);
        entity.setTeacherId(teacherId);
        entity.setStatus(status);
        entity.setScheduledStart(LocalDateTime.now().plusDays(3));
        entity.setScheduledEnd(LocalDateTime.now().plusDays(3).plusHours(1));
        entity.setDurationMinutes(60);
        entity.setPrice(new BigDecimal("300.00"));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
