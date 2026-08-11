package com.neopick.adapter.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neopick.application.booking.ManageBookingUseCase;
import com.neopick.application.booking.SubmitBookingUseCase;
import com.neopick.application.booking.GetStudentBookingsUseCase;
import com.neopick.application.booking.GetTeacherBookingsUseCase;
import com.neopick.domain.booking.*;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import({SubmitBookingUseCase.class, ManageBookingUseCase.class,
         GetStudentBookingsUseCase.class, GetTeacherBookingsUseCase.class})
@DisplayName("Booking API Integration Tests")
class BookingControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private BookingRepository bookingRepository;
    @MockBean private SecurityContext securityContext;
    @MockBean private com.neopick.infrastructure.metrics.BusinessMetrics businessMetrics;

    private static final String STUDENT_ID = "student-001";
    private static final Long TEACHER_ID = 100L;
    private static final UUID BOOKING_UUID = UUID.randomUUID();
    private static final BookingId BOOKING_ID = new BookingId(BOOKING_UUID);

    private Booking pendingConfirmBooking;

    @BeforeEach
    void setUp() {
        when(securityContext.requireCurrentUserId()).thenReturn(STUDENT_ID);
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of(STUDENT_ID));

        pendingConfirmBooking = new Booking(
                BOOKING_ID, STUDENT_ID, TEACHER_ID,
                LocalDateTime.now().plusDays(3).withHour(14).withMinute(0),
                LocalDateTime.now().plusDays(3).withHour(15).withMinute(0),
                60, new BigDecimal("300.00"),
                new Address("Home", "Room 101", 31.2304, 121.4737),
                "Please focus on fingerstyle technique");
    }

    @Nested
    @DisplayName("POST /api/v1/bookings — Submit booking")
    class SubmitBooking {

        @Test
        @DisplayName("should create booking with PENDING_CONFIRM status")
        void shouldCreateBooking() throws Exception {
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                return b;
            });

            String body = """
                    {
                        "teacher_id": 100,
                        "scheduled_start": "%s",
                        "duration_minutes": 60,
                        "price": 300.00,
                        "address_label": "Home",
                        "address_detail": "Room 101",
                        "latitude": 31.2304,
                        "longitude": 121.4737,
                        "note": "Please focus on fingerstyle technique"
                    }""".formatted(LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).toString());

            mockMvc.perform(post("/api/v1/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRM"))
                    .andExpect(jsonPath("$.data.teacher_id").value(100))
                    .andExpect(jsonPath("$.data.price").value(300.00))
                    .andExpect(jsonPath("$.data.address.label").value("Home"));
        }

        @Test
        @DisplayName("should reject booking without teacher_id")
        void shouldRejectMissingTeacherId() throws Exception {
            String body = """
                    {
                        "scheduled_start": "%s",
                        "duration_minutes": 60,
                        "price": 300.00,
                        "address_label": "Home",
                        "latitude": 31.2304,
                        "longitude": 121.4737
                    }""".formatted(LocalDateTime.now().plusDays(3).toString());

            mockMvc.perform(post("/api/v1/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Booking lifecycle — full state machine flow")
    class BookingLifecycle {

        @Test
        @DisplayName("PENDING_CONFIRM → confirm → PENDING_PAY → pay → PENDING_CLASS → complete → COMPLETED")
        void shouldTransitionThroughFullLifecycle() throws Exception {
            Booking booking = new Booking(
                    BOOKING_ID, STUDENT_ID, TEACHER_ID,
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(0),
                    LocalDateTime.now().plusDays(3).withHour(15).withMinute(0),
                    60, new BigDecimal("300.00"),
                    new Address("Studio A", "Floor 2", 31.2304, 121.4737),
                    "Test booking");

            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            // Step 1: Confirm — PENDING_CONFIRM → PENDING_PAY
            mockMvc.perform(put("/api/v1/bookings/{id}/confirm", BOOKING_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING_PAY"));

            // Step 2: Pay (domain method — payment happens via PaymentController in production)
            booking.pay();

            // Step 3: Complete — PENDING_CLASS → COMPLETED
            mockMvc.perform(put("/api/v1/bookings/{id}/complete", BOOKING_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should fail to confirm a booking not in PENDING_CONFIRM")
        void shouldFailInvalidConfirmTransition() throws Exception {
            Booking completed = new Booking(
                    BOOKING_ID, STUDENT_ID, TEACHER_ID,
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(0),
                    LocalDateTime.now().plusDays(3).withHour(15).withMinute(0),
                    60, new BigDecimal("300.00"),
                    new Address("Studio A", "Floor 2", 31.2304, 121.4737),
                    "Test booking");
            completed.confirm(); // PENDING_CONFIRM → PENDING_PAY
            completed.pay();     // PENDING_PAY → PENDING_CLASS
            completed.complete(); // PENDING_CLASS → COMPLETED

            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(completed));

            mockMvc.perform(put("/api/v1/bookings/{id}/confirm", BOOKING_UUID))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PENDING_CONFIRM → cancel → CANCELLED")
        void shouldCancelPendingBooking() throws Exception {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(pendingConfirmBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(put("/api/v1/bookings/{id}/cancel", BOOKING_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Schedule conflict\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.data.cancel_reason").value("Schedule conflict"));
        }

        @Test
        @DisplayName("PENDING_CONFIRM → teacher rejects → CANCELLED")
        void shouldRejectByTeacher() throws Exception {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(pendingConfirmBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(put("/api/v1/bookings/{id}/reject", BOOKING_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Time slot unavailable\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.data.cancelled_by").value("TEACHER"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bookings — List student bookings")
    class ListBookings {

        @Test
        @DisplayName("should return paginated booking list")
        void shouldReturnPaginatedBookings() throws Exception {
            when(bookingRepository.findByStudentId(STUDENT_ID, null, 0, 20))
                    .thenReturn(List.of(pendingConfirmBooking));
            when(bookingRepository.countByStudentId(STUDENT_ID, null)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/bookings")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].status").value("PENDING_CONFIRM"))
                    .andExpect(jsonPath("$.data.items[0].teacher_id").value(100))
                    .andExpect(jsonPath("$.data.total").value(1));
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() throws Exception {
            when(bookingRepository.findByStudentId(STUDENT_ID, BookingStatus.COMPLETED, 0, 20))
                    .thenReturn(List.of());
            when(bookingRepository.countByStudentId(STUDENT_ID, BookingStatus.COMPLETED)).thenReturn(0L);

            mockMvc.perform(get("/api/v1/bookings")
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bookings/teacher — List teacher bookings")
    class ListTeacherBookings {

        @Test
        @DisplayName("should return teacher's bookings")
        void shouldReturnTeacherBookings() throws Exception {
            when(bookingRepository.findByTeacherId(TEACHER_ID, null, 0, 20))
                    .thenReturn(List.of(pendingConfirmBooking));
            when(bookingRepository.countByTeacherId(TEACHER_ID, null)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/bookings/teacher")
                            .param("teacher_id", TEACHER_ID.toString())
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].student_id").value(STUDENT_ID));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bookings/{id} — Get booking detail")
    class GetBookingDetail {

        @Test
        @DisplayName("should return booking by id")
        void shouldReturnBooking() throws Exception {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(pendingConfirmBooking));

            mockMvc.perform(get("/api/v1/bookings/{id}", BOOKING_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(BOOKING_UUID.toString()))
                    .andExpect(jsonPath("$.data.scheduled_start", notNullValue()))
                    .andExpect(jsonPath("$.data.duration_minutes").value(60));
        }

        @Test
        @DisplayName("should return 400 when booking not found")
        void shouldReturnNotFound() throws Exception {
            when(bookingRepository.findById(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/bookings/{id}", UUID.randomUUID()))
                    .andExpect(status().isBadRequest());
        }
    }
}
