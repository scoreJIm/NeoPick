package com.neopick.adapter.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neopick.BaseIntegrationTest;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.booking.BookingStatus;
import com.neopick.domain.user.User;
import com.neopick.domain.user.UserId;
import com.neopick.domain.user.UserRepository;
import com.neopick.domain.user.PhoneNumber;
import com.neopick.domain.user.UserRole;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@DisplayName("Booking API — Real DB E2E Tests")
class BookingE2ETest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;

    @MockBean private SecurityContext securityContext;

    private String studentId;
    private static final Long TEACHER_ID = 100L;

    @BeforeEach
    void setUp() {
        User student = new User(UserId.generate(), PhoneNumber.of("13800138001"),
                "TestStudent", UserRole.STUDENT);
        User saved = userRepository.save(student);
        studentId = saved.getId().value().toString();

        when(securityContext.requireCurrentUserId()).thenReturn(studentId);
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of(studentId));
    }

    @Test
    @DisplayName("Full booking lifecycle: submit → confirm → complete → verify persisted")
    void fullBookingLifecycle() throws Exception {
        String scheduleTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0)
                .withSecond(0).withNano(0).toString();

        // Step 1: Submit booking
        String submitBody = """
                {
                    "teacher_id": 100,
                    "scheduled_start": "%s",
                    "duration_minutes": 60,
                    "price": 300.00,
                    "address_label": "Studio A",
                    "address_detail": "Floor 2, Room 3",
                    "latitude": 31.2304,
                    "longitude": 121.4737,
                    "note": "Focus on fingerpicking"
                }""".formatted(scheduleTime);

        String submitResponse = mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRM"))
                .andExpect(jsonPath("$.data.price").value(300.00))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(submitResponse).get("data").get("id").asText();

        // Step 2: Verify persisted in DB
        var persisted = bookingRepository.findById(
                com.neopick.domain.booking.BookingId.from(bookingId));
        assert persisted.isPresent() : "Booking should be persisted";
        assert persisted.get().getStatus() == BookingStatus.PENDING_CONFIRM;

        // Step 3: Teacher confirms → PENDING_PAY
        mockMvc.perform(put("/api/v1/bookings/{id}/confirm", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAY"))
                .andExpect(jsonPath("$.data.confirmed_at", notNullValue()));

        // Step 4: Verify state change persisted
        persisted = bookingRepository.findById(
                com.neopick.domain.booking.BookingId.from(bookingId));
        assert persisted.get().getStatus() == BookingStatus.PENDING_PAY;

        // Step 5: Complete (after simulated payment via domain method)
        persisted.get().pay(); // transition PENDING_PAY → PENDING_CLASS
        bookingRepository.save(persisted.get());

        mockMvc.perform(put("/api/v1/bookings/{id}/complete", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completed_at", notNullValue()));

        // Step 6: Get detail and verify final state
        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bookingId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.teacher_id").value(100))
                .andExpect(jsonPath("$.data.duration_minutes").value(60))
                .andExpect(jsonPath("$.data.address.label").value("Studio A"));
    }

    @Test
    @DisplayName("Cancel flow: submit → cancel → verify CANCELLED state")
    void cancelFlow() throws Exception {
        String scheduleTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0)
                .withSecond(0).withNano(0).toString();

        String submitBody = """
                {
                    "teacher_id": 100,
                    "scheduled_start": "%s",
                    "duration_minutes": 45,
                    "price": 200.00,
                    "address_label": "Home",
                    "address_detail": "123 Main St",
                    "latitude": 31.2304,
                    "longitude": 121.4737
                }""".formatted(scheduleTime);

        String submitResponse = mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(submitResponse).get("data").get("id").asText();

        // Cancel
        mockMvc.perform(put("/api/v1/bookings/{id}/cancel", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Schedule changed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancel_reason").value("Schedule changed"))
                .andExpect(jsonPath("$.data.cancelled_by").value(studentId));

        // Verify cancelled booking cannot be confirmed
        mockMvc.perform(put("/api/v1/bookings/{id}/confirm", bookingId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("List student bookings — should return persisted bookings")
    void listStudentBookings() throws Exception {
        // Submit two bookings
        for (int i = 0; i < 2; i++) {
            String scheduleTime = LocalDateTime.now().plusDays(4 + i).withHour(14).withMinute(0)
                    .withSecond(0).withNano(0).toString();
            mockMvc.perform(post("/api/v1/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "teacher_id": 100,
                                "scheduled_start": "%s",
                                "duration_minutes": 60,
                                "price": 250.00,
                                "address_label": "Studio",
                                "latitude": 31.2304,
                                "longitude": 121.4737
                            }""".formatted(scheduleTime)))
                    .andExpect(status().isOk());
        }

        // List should return both
        mockMvc.perform(get("/api/v1/bookings")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(2));
    }
}
