package com.programandoenjava.airline.checkin.application.port.in.checkin;

import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

/**
 * The booking is all a passenger sends. Who they are and what they are flying
 * on are already recorded against it, and taking either from the request would
 * let a caller check in for a flight it never booked.
 */
public record CheckInCommand(BookingId bookingId) {

    public CheckInCommand {
        if (bookingId == null) {
            throw new DomainValidationException("A check-in must name a booking");
        }
    }
}
