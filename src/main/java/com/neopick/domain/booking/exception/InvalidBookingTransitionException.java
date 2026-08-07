package com.neopick.domain.booking;

import com.neopick.domain.common.BusinessException;

public class InvalidBookingTransitionException extends BusinessException {

    public InvalidBookingTransitionException(BookingStatus current, String action) {
        super("BOOKING_INVALID_TRANSITION",
                "Cannot " + action + " booking in status " + current);
    }
}
