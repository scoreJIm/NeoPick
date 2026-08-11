package com.neopick.domain.booking;

import com.neopick.domain.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Booking State Machine")
class BookingTest {

    private static final BookingId BOOKING_ID = BookingId.generate();
    private static final String STUDENT_ID = "student-001";
    private static final Long TEACHER_ID = 100L;
    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = new Booking(BOOKING_ID, STUDENT_ID, TEACHER_ID,
                LocalDateTime.now().plusDays(3).withHour(14).withMinute(0),
                LocalDateTime.now().plusDays(3).withHour(15).withMinute(0),
                60, new BigDecimal("300.00"),
                new Address("Home", "Room 101", 31.2304, 121.4737),
                "Please focus on technique");
    }

    @Nested
    @DisplayName("Initial state")
    class InitialState {

        @Test
        @DisplayName("new booking should be PENDING_CONFIRM")
        void newBookingShouldBePendingConfirm() {
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_CONFIRM);
        }

        @Test
        @DisplayName("should set created_at on creation")
        void shouldSetCreatedAt() {
            assertThat(booking.getCreatedAt()).isNotNull();
            assertThat(booking.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should set correct fields from constructor")
        void shouldSetCorrectFields() {
            assertThat(booking.getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(booking.getTeacherId()).isEqualTo(TEACHER_ID);
            assertThat(booking.getDurationMinutes()).isEqualTo(60);
            assertThat(booking.getPrice()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(booking.getAddress().label()).isEqualTo("Home");
            assertThat(booking.getAddress().latitude()).isEqualTo(31.2304);
        }
    }

    @Nested
    @DisplayName("Transition: PENDING_CONFIRM → confirm() → PENDING_PAY")
    class ConfirmTransition {

        @Test
        @DisplayName("should transition to PENDING_PAY when confirmed")
        void shouldTransitionToPendingPay() {
            booking.confirm();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAY);
            assertThat(booking.getConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("should update timestamp on confirm")
        void shouldUpdateTimestampOnConfirm() {
            LocalDateTime before = booking.getUpdatedAt();
            booking.confirm();
            assertThat(booking.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("Transition: PENDING_CONFIRM → reject() → CANCELLED")
    class RejectTransition {

        @Test
        @DisplayName("should transition to CANCELLED when rejected")
        void shouldTransitionToCancelledWhenRejected() {
            booking.reject("Time slot unavailable");
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.getCancelReason()).isEqualTo("Time slot unavailable");
            assertThat(booking.getCancelledBy()).isEqualTo("TEACHER");
            assertThat(booking.getCancelledAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Transition: PENDING_PAY → pay() → PENDING_CLASS")
    class PayTransition {

        @Test
        @DisplayName("should transition to PENDING_CLASS when paid")
        void shouldTransitionToPendingClass() {
            booking.confirm();
            booking.pay();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_CLASS);
            assertThat(booking.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw when paying from PENDING_CONFIRM")
        void shouldThrowWhenPayingFromPendingConfirm() {
            assertThatThrownBy(() -> booking.pay())
                    .isInstanceOf(InvalidBookingTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Transition: PENDING_CLASS → complete() → COMPLETED")
    class CompleteTransition {

        @Test
        @DisplayName("should transition to COMPLETED")
        void shouldTransitionToCompleted() {
            booking.confirm();
            booking.pay();
            booking.complete();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
            assertThat(booking.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw when completing from PENDING_PAY")
        void shouldThrowWhenCompletingFromPendingPay() {
            booking.confirm();
            assertThatThrownBy(() -> booking.complete())
                    .isInstanceOf(InvalidBookingTransitionException.class);
        }

        @Test
        @DisplayName("completed booking CAN be reviewed")
        void completedBookingCanBeReviewed() {
            booking.confirm();
            booking.pay();
            booking.complete();
            assertThat(booking.canBeReviewed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Transition: cancel() from various states")
    class CancelTransition {

        @Test
        @DisplayName("should cancel from PENDING_CONFIRM")
        void shouldCancelFromPendingConfirm() {
            booking.cancel("Changed my mind", STUDENT_ID);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.getCancelledBy()).isEqualTo(STUDENT_ID);
        }

        @Test
        @DisplayName("should cancel from PENDING_PAY")
        void shouldCancelFromPendingPay() {
            booking.confirm();
            booking.cancel("Too expensive", STUDENT_ID);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("should cancel from PENDING_CLASS")
        void shouldCancelFromPendingClass() {
            booking.confirm();
            booking.pay();
            booking.cancel("Schedule conflict", STUDENT_ID);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("should NOT cancel COMPLETED booking")
        void shouldNotCancelCompleted() {
            booking.confirm();
            booking.pay();
            booking.complete();
            assertThatThrownBy(() -> booking.cancel("Too late", STUDENT_ID))
                    .isInstanceOf(InvalidBookingTransitionException.class);
        }

        @Test
        @DisplayName("should NOT cancel already CANCELLED booking")
        void shouldNotCancelAlreadyCancelled() {
            booking.cancel("First cancel", STUDENT_ID);
            assertThatThrownBy(() -> booking.cancel("Second cancel", STUDENT_ID))
                    .isInstanceOf(InvalidBookingTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Invalid transitions from wrong states")
    class InvalidTransitions {

        @Test
        @DisplayName("should throw when confirming from CANCELLED")
        void shouldThrowConfirmingCancelled() {
            booking.cancel("Test", STUDENT_ID);
            assertThatThrownBy(() -> booking.confirm())
                    .isInstanceOf(InvalidBookingTransitionException.class);
        }

        @Test
        @DisplayName("should throw when rejecting from PENDING_PAY")
        void shouldThrowRejectingPendingPay() {
            booking.confirm();
            assertThatThrownBy(() -> booking.reject("test"))
                    .isInstanceOf(InvalidBookingTransitionException.class);
        }
    }

    @Nested
    @DisplayName("canBeReviewed")
    class CanBeReviewed {

        @Test
        @DisplayName("should return false for PENDING_CONFIRM")
        void pendingConfirmCannotBeReviewed() {
            assertThat(booking.canBeReviewed()).isFalse();
        }

        @Test
        @DisplayName("should return false for PENDING_PAY")
        void pendingPayCannotBeReviewed() {
            booking.confirm();
            assertThat(booking.canBeReviewed()).isFalse();
        }

        @Test
        @DisplayName("should return false for PENDING_CLASS")
        void pendingClassCannotBeReviewed() {
            booking.confirm();
            booking.pay();
            assertThat(booking.canBeReviewed()).isFalse();
        }

        @Test
        @DisplayName("should return false for CANCELLED")
        void cancelledCannotBeReviewed() {
            booking.cancel("Test", STUDENT_ID);
            assertThat(booking.canBeReviewed()).isFalse();
        }

        @Test
        @DisplayName("should return true only for COMPLETED")
        void onlyCompletedCanBeReviewed() {
            booking.confirm();
            booking.pay();
            booking.complete();
            assertThat(booking.canBeReviewed()).isTrue();
        }
    }
}
