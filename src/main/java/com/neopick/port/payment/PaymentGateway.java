package com.neopick.port.payment;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.payment.Payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Port interface for payment gateway integrations (Alipay, WeChat, etc.).
 */
public interface PaymentGateway {

    /**
     * Initiate a payment with the payment provider.
     *
     * @param payment   the payment domain object with ID, amount, method
     * @param booking   the associated booking for subject/body details
     * @return result containing pay URL and gateway transaction number
     */
    InitiatePaymentResult initiatePayment(Payment payment, Booking booking);

    /**
     * Verify the authenticity of an async payment callback from the provider.
     *
     * @param params all request parameters from the callback
     * @return verified callback data
     */
    CallbackResult verifyCallback(Map<String, String> params);

    /**
     * Query the current status of a payment from the provider.
     *
     * @param outTradeNo the merchant order number
     * @return current payment status from the gateway
     */
    QueryResult queryPayment(String outTradeNo);

    /**
     * Refund a payment (full or partial).
     *
     * @param outTradeNo the merchant order number
     * @param amount     refund amount
     * @param reason     reason for refund
     * @return refund result
     */
    RefundResult refundPayment(String outTradeNo, BigDecimal amount, String reason);

    /**
     * Return the payment method this gateway handles.
     */
    String supportedMethod();

    record InitiatePaymentResult(boolean success, String tradeNo, String payUrl, String qrCode) {}

    record CallbackResult(boolean success, boolean signatureValid, String tradeNo,
                          String outTradeNo, BigDecimal totalAmount, String tradeStatus) {}

    record QueryResult(boolean success, String tradeNo, String outTradeNo,
                       BigDecimal totalAmount, String tradeStatus) {}

    record RefundResult(boolean success, String refundTradeNo, String outTradeNo,
                        BigDecimal refundAmount) {}
}
