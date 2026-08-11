package com.neopick.domain.booking;

import com.neopick.domain.common.AggregateRoot;
import com.neopick.domain.common.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Booking implements AggregateRoot {

    private BookingId id;
    private String studentId;
    private Long teacherId;
    private BookingStatus status;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private int durationMinutes;
    private BigDecimal price;
    private Address address;
    private String studentNote;
    private String cancelReason;
    private String cancelledBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Booking() {
    }

    public Booking(BookingId id, String studentId, Long teacherId, LocalDateTime scheduledStart,
                   LocalDateTime scheduledEnd, int durationMinutes, BigDecimal price,
                   Address address, String studentNote) {
        this.id = id;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.address = address;
        this.studentNote = studentNote;
        this.status = BookingStatus.PENDING_CONFIRM;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    static Booking reconstruct(BookingId id, String studentId, Long teacherId,
            BookingStatus status, LocalDateTime scheduledStart, LocalDateTime scheduledEnd,
            int durationMinutes, BigDecimal price, Address address, String studentNote,
            String cancelReason, String cancelledBy, LocalDateTime confirmedAt,
            LocalDateTime paidAt, LocalDateTime completedAt, LocalDateTime cancelledAt,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        Booking b = new Booking();
        b.id = id;
        b.studentId = studentId;
        b.teacherId = teacherId;
        b.status = status;
        b.scheduledStart = scheduledStart;
        b.scheduledEnd = scheduledEnd;
        b.durationMinutes = durationMinutes;
        b.price = price;
        b.address = address;
        b.studentNote = studentNote;
        b.cancelReason = cancelReason;
        b.cancelledBy = cancelledBy;
        b.confirmedAt = confirmedAt;
        b.paidAt = paidAt;
        b.completedAt = completedAt;
        b.cancelledAt = cancelledAt;
        b.createdAt = createdAt;
        b.updatedAt = updatedAt;
        return b;
    }

    public void confirm() {
        assertStatus(BookingStatus.PENDING_CONFIRM, "confirm");
        this.status = BookingStatus.PENDING_PAY;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        assertStatus(BookingStatus.PENDING_CONFIRM, "reject");
        this.status = BookingStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledBy = "TEACHER";
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void pay() {
        assertStatus(BookingStatus.PENDING_PAY, "pay");
        this.status = BookingStatus.PENDING_CLASS;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        assertStatus(BookingStatus.PENDING_CLASS, "complete");
        this.status = BookingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason, String cancelledBy) {
        if (this.status == BookingStatus.COMPLETED) {
            throw new InvalidBookingTransitionException(this.status, "cancel");
        }
        if (this.status == BookingStatus.CANCELLED) {
            throw new InvalidBookingTransitionException(this.status, "cancel");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canBeReviewed() {
        return this.status == BookingStatus.COMPLETED;
    }

    private void assertStatus(BookingStatus expected, String action) {
        if (this.status != expected) {
            throw new InvalidBookingTransitionException(this.status, action);
        }
    }

    public BookingId getId() { return id; }
    public String getStudentId() { return studentId; }
    public Long getTeacherId() { return teacherId; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public LocalDateTime getScheduledEnd() { return scheduledEnd; }
    public int getDurationMinutes() { return durationMinutes; }
    public BigDecimal getPrice() { return price; }
    public Address getAddress() { return address; }
    public String getStudentNote() { return studentNote; }
    public String getCancelReason() { return cancelReason; }
    public String getCancelledBy() { return cancelledBy; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
