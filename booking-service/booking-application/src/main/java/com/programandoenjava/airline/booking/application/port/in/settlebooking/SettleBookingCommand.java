package com.programandoenjava.airline.booking.application.port.in.settlebooking;

import com.programandoenjava.airline.booking.domain.booking.BookingId;

import java.util.UUID;

/**
 * The event id travels with the command because deduplication is the use case's
 * business, not the consumer's: what must not happen twice is the work, and only
 * the use case knows what the work was.
 */
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
