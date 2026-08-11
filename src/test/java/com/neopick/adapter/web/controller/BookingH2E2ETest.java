package com.neopick.adapter.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.booking.BookingStatus;
import com.neopick.domain.user.*;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Booking E2E — H2 (no Docker)")
class BookingH2E2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;

    @MockBean private SecurityContext securityContext;

    private String studentId;

    @BeforeEach
    void setUp() {
        User student = new User(UserId.generate(), PhoneNumber.of("13800138003"),
                "H2Tester", UserRole.STUDENT);
        User saved = userRepository.save(student);
        studentId = saved.getId().value().toString();

        when(securityContext.requireCurrentUserId()).thenReturn(studentId);
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of(studentId));
    }

    @Test
    @DisplayName("Submit → persist → verify persisted state")
    void shouldPersistBookingToH2() throws Exception {
        String scheduleTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0)
                .withSecond(0).withNano(0).toString();

        String response = mockMvc.perform(post("/api/v1/bookings")
                        .contentType("application/json")
                        .content(bookingBody(scheduleTime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRM"))
                .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(response).get("data").get("id").asText();

        var persisted = bookingRepository.findById(
                com.neopick.domain.booking.BookingId.from(bookingId));
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getStatus()).isEqualTo(BookingStatus.PENDING_CONFIRM);
        assertThat(persisted.get().getStudentId()).isEqualTo(studentId);
        assertThat(persisted.get().getTeacherId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Submit → confirm → complete → verify each state persisted")
    void fullLifecycleWithDbVerification() throws Exception {
        String scheduleTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0)
                .withSecond(0).withNano(0).toString();

        String response = mockMvc.perform(post("/api/v1/bookings")
                        .contentType("application/json")
                        .content(bookingBody(scheduleTime)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(response).get("data").get("id").asText();

        // Confirm → verify DB
        mockMvc.perform(put("/api/v1/bookings/{id}/confirm", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAY"));

        var afterConfirm = bookingRepository.findById(
                com.neopick.domain.booking.BookingId.from(bookingId));
        assertThat(afterConfirm).isPresent();
        assertThat(afterConfirm.get().getStatus()).isEqualTo(BookingStatus.PENDING_PAY);

        // Pay + Complete → verify DB
        afterConfirm.get().pay();
        bookingRepository.save(afterConfirm.get());

        mockMvc.perform(put("/api/v1/bookings/{id}/complete", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        var afterComplete = bookingRepository.findById(
                com.neopick.domain.booking.BookingId.from(bookingId));
        assertThat(afterComplete.get().getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(afterComplete.get().getCompletedAt()).isNotNull();
    }

    private String bookingBody(String scheduleTime) {
        return """
                {
                    "teacher_id": 100,
                    "scheduled_start": "%s",
                    "duration_minutes": 60,
                    "price": 300.00,
                    "address_label": "H2 Studio",
                    "address_detail": "Test Room",
                    "latitude": 31.2304,
                    "longitude": 121.4737
                }""".formatted(scheduleTime);
    }
}
