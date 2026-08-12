package com.neopick.adapter.web.controller;

import com.neopick.application.payment.HandlePaymentCallbackUseCase;
import com.neopick.application.payment.InitiatePaymentUseCase;
import com.neopick.domain.booking.*;
import com.neopick.domain.payment.*;
import com.neopick.port.payment.PaymentGateway;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(InitiatePaymentUseCase.class)
@DisplayName("Payment API Integration Tests")
class PaymentControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private PaymentRepository paymentRepository;
    @MockBean private BookingRepository bookingRepository;
    @MockBean private SecurityContext securityContext;
    @MockBean private com.neopick.infrastructure.metrics.BusinessMetrics businessMetrics;
    @MockBean private PaymentGateway paymentGateway;
    @MockBean private HandlePaymentCallbackUseCase handlePaymentCallbackUseCase;

    private static final UUID BOOKING_UUID = UUID.randomUUID();
    private static final UUID PAYMENT_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(securityContext.requireCurrentUserId()).thenReturn("student-001");
        when(paymentGateway.supportedMethod()).thenReturn("WECHAT");
        when(paymentGateway.initiatePayment(any(), any()))
                .thenReturn(new PaymentGateway.InitiatePaymentResult(
                        true, "txn-001", "https://pay.example.com/order/test", null));
    }

    @Nested
    @DisplayName("POST /api/v1/payments - Initiate payment")
    class InitiatePayment {

        @Test
        @DisplayName("should create payment and return pay URL")
        void shouldInitiatePayment() throws Exception {
            Booking booking = new Booking(
                    new BookingId(BOOKING_UUID), "student-001", 100L,
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(0),
                    LocalDateTime.now().plusDays(3).withHour(15).withMinute(0),
                    60, new BigDecimal("300.00"),
                    new Address("Home", "Room 101", 31.2304, 121.4737), "Test");

            when(bookingRepository.findById(any(BookingId.class))).thenReturn(Optional.of(booking));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"booking_id": "%s", "method": "WECHAT"}""".formatted(BOOKING_UUID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.method").value("WECHAT"))
                    .andExpect(jsonPath("$.data.pay_url").value(containsString("https://pay.example.com")));
        }

        @Test
        @DisplayName("should reject payment without booking_id")
        void shouldRejectMissingBookingId() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\": \"WECHAT\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail when booking not found")
        void shouldFailWhenBookingNotFound() throws Exception {
            when(bookingRepository.findById(any(BookingId.class))).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"booking_id": "%s", "method": "WECHAT"}""".formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
        }
    }
}
