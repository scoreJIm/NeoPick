package com.neopick.adapter.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neopick.BaseIntegrationTest;
import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.payment.Payment;
import com.neopick.domain.payment.PaymentId;
import com.neopick.domain.payment.PaymentMethod;
import com.neopick.domain.payment.PaymentRepository;
import com.neopick.domain.review.Review;
import com.neopick.domain.review.ReviewId;
import com.neopick.domain.review.ReviewRepository;
import com.neopick.domain.user.*;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Payment + Review — Real DB E2E")
class PaymentReviewE2ETest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;

    @MockBean private SecurityContext securityContext;

    private String studentId;
    private static final Long TEACHER_ID = 100L;

    @BeforeEach
    void setUp() {
        User student = new User(UserId.generate(), PhoneNumber.of("13800138002"),
                "E2ETester", UserRole.STUDENT);
        User saved = userRepository.save(student);
        studentId = saved.getId().value().toString();

        when(securityContext.requireCurrentUserId()).thenReturn(studentId);
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of(studentId));
    }

    @Test
    @DisplayName("Full journey: book → confirm → pay → complete → review")
    void fullPaymentReviewJourney() throws Exception {
        String scheduleTime = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0)
                .withSecond(0).withNano(0).toString();

        // Step 1: Submit booking
        String bookingJson = mockMvc.perform(post("/api/v1/bookings")
                        .contentType("application/json")
                        .content(bookingBody(scheduleTime)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String bookingId = objectMapper.readTree(bookingJson).get("data").get("id").asText();

        // Step 2: Teacher confirms
        mockMvc.perform(put("/api/v1/bookings/{id}/confirm", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAY"));

        // Step 3: Initiate payment
        String paymentJson = mockMvc.perform(post("/api/v1/payments")
                        .contentType("application/json")
                        .content("""
                                {"booking_id":"%s","method":"WECHAT"}""".formatted(bookingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.method").value("WECHAT"))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).get("data").get("id").asText();

        // Step 4: Verify payment persisted
        Optional<Payment> savedPayment = paymentRepository.findById(
                PaymentId.from(UUID.fromString(paymentId)));
        assertThat(savedPayment).isPresent();
        assertThat(savedPayment.get().getBookingId()).isEqualTo(bookingId);

        // Step 5: Mark payment as paid and complete booking
        Booking booking = bookingRepository.findById(BookingId.from(bookingId)).orElseThrow();
        booking.pay(); // Simulate payment callback
        bookingRepository.save(booking);

        mockMvc.perform(put("/api/v1/bookings/{id}/complete", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Step 6: Submit review
        mockMvc.perform(post("/api/v1/reviews")
                        .contentType("application/json")
                        .content("""
                                {
                                    "booking_id":"%s",
                                    "teacher_id":100,
                                    "rating":5,
                                    "content":"E2E test review — excellent!",
                                    "tags":["e2e","test"]
                                }""".formatted(bookingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.content").value("E2E test review — excellent!"));

        // Step 7: Verify review persisted and is unique
        assertThat(reviewRepository.existsByBookingId(bookingId)).isTrue();

        // Step 8: Verify review appears in my reviews
        mockMvc.perform(get("/api/v1/reviews/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].booking_id").value(bookingId));
    }

    private String bookingBody(String scheduleTime) {
        return """
                {
                    "teacher_id": 100,
                    "scheduled_start": "%s",
                    "duration_minutes": 60,
                    "price": 300.00,
                    "address_label": "Music Studio",
                    "address_detail": "Floor 3, Room 5",
                    "latitude": 31.2304,
                    "longitude": 121.4737,
                    "note": "E2E payment+review flow test"
                }""".formatted(scheduleTime);
    }
}
