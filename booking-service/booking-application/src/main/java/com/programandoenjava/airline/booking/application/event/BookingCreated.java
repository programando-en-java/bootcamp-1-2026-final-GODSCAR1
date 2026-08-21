package com.programandoenjava.airline.booking.application.event;

import com.programandoenjava.airline.booking.domain.booking.Booking;

public record BookingCreated(Booking booking) {

    public BookingCreated {
        if (booking == null) {
            throw new IllegalArgumentException("A booking is required");
        }
    }
}
