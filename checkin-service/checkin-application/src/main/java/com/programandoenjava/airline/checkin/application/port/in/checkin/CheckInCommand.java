package com.programandoenjava.airline.checkin.application.port.in.checkin;

import com.programandoenjava.airline.checkin.application.port.shared.Caller;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

public record CheckInCommand(BookingId bookingId, Caller caller) {

    public CheckInCommand {
        if (bookingId == null) {
            throw new DomainValidationException("A check-in must name a booking");
        }
        if (caller == null) {
            throw new DomainValidationException("A check-in must say who is asking");
        }
    }
}
