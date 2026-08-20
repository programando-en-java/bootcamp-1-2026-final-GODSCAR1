package com.programandoenjava.airline.booking.application.event;

import com.programandoenjava.airline.booking.domain.booking.Booking;

/**
 * A booking was made. In process only: it carries value objects and is never
 * serialised. What crosses the wire is the flat integration event the listener
 * maps this into (ADR-001).
 */
public record BookingCreated(Booking booking) {

    public BookingCreated {
        if (booking == null) {
            throw new IllegalArgumentException("A booking is required");
        }
    }
}
