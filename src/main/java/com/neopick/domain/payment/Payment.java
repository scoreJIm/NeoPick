package com.neopick.domain.payment;

import com.neopick.domain.common.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment implements AggregateRoot {

    private PaymentId id;
    private String bookingId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Payment() {
    }

    public Payment(PaymentId id, String bookingId, BigDecimal amount, PaymentMethod method) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    static Payment reconstruct(PaymentId id, String bookingId, BigDecimal amount,
            PaymentMethod method, PaymentStatus status, String transactionId,
            LocalDateTime paidAt, LocalDateTime refundedAt,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        Payment p = new Payment();
        p.id = id;
        p.bookingId = bookingId;
        p.amount = amount;
        p.method = method;
        p.status = status;
        p.transactionId = transactionId;
        p.paidAt = paidAt;
        p.refundedAt = refundedAt;
        p.createdAt = createdAt;
        p.updatedAt = updatedAt;
        return p;
    }

    public void markPaid(String transactionId) {
        this.status = PaymentStatus.PAID;
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public PaymentId getId() { return id; }
    public String getBookingId() { return bookingId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
