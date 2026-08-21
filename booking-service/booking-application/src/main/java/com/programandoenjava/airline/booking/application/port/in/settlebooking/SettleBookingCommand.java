package com.programandoenjava.airline.booking.application.port.in.settlebooking;

import com.programandoenjava.airline.booking.domain.booking.BookingId;

import java.util.UUID;

public record SettleBookingCommand(UUID eventId, BookingId bookingId) {

    public SettleBookingCommand {
        if (eventId == null) {
            throw new IllegalArgumentException("An event id is required");
        }
        if (bookingId == null) {
            throw new IllegalArgumentException("A booking must be named");
        }
    }
}
