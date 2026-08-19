package com.programandoenjava.airline.checkin.application.port.out.readbooking;

import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import com.programandoenjava.airline.checkin.domain.boardingpass.PassengerId;
import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

/**
 * The little of a booking that check-in needs. The status arrives as the text
 * booking-service sent rather than an enum of our own: this service does not
 * decide what states a booking has, and an unknown one should read as "not
 * confirmed" instead of failing to deserialise.
 */
public record BookingToCheckIn(BookingId id,
                               PassengerId passengerId,
                               FlightId flightId,
                               String status) {

    private static final String CONFIRMED = "CONFIRMED";

    public BookingToCheckIn {
        if (id == null || passengerId == null || flightId == null) {
            throw new DomainValidationException(
                    "A booking to check in needs a booking, a passenger and a flight");
        }
        if (status == null || status.isBlank()) {
            throw new DomainValidationException("A booking to check in needs a status");
        }
    }

    public boolean isConfirmed() {
        return CONFIRMED.equals(status);
    }
}
