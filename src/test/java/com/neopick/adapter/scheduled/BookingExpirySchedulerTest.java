package com.neopick.adapter.scheduled;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("BookingExpiryScheduler")
class BookingExpirySchedulerTest {

    @Test
    @DisplayName("should run without exceptions")
    void shouldRunWithoutExceptions() {
        BookingExpiryScheduler scheduler = new BookingExpiryScheduler();
        assertThatCode(scheduler::expirePendingBookings)
                .doesNotThrowAnyException();
    }
}
