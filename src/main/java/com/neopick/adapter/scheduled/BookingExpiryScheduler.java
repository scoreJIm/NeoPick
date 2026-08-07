package com.neopick.adapter.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    @Scheduled(fixedRate = 600000)
    public void expirePendingBookings() {
        log.debug("Checking for expired bookings...");
    }
}
