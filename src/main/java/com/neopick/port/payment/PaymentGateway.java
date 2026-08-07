package com.neopick.port.payment;

public interface PaymentGateway {

    PaymentResult initiate(String orderId, String amount, String description);

    PaymentStatus queryStatus(String transactionId);

    RefundResult refund(String transactionId, String amount, String reason);

    record PaymentResult(boolean success, String transactionId, String payUrl) {}

    record RefundResult(boolean success, String refundTransactionId) {}

    enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }
}
