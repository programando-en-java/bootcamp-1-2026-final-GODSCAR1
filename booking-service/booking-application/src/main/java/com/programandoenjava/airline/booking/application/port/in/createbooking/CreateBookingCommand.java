package com.programandoenjava.airline.booking.application.port.in.createbooking;

import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.PassengerId;
import com.programandoenjava.airline.booking.domain.booking.SeatCount;

public record CreateBookingCommand(PassengerId passengerId,
                                   FlightId flightId,
                                   SeatCount seats,
                                   IdempotencyKey idempotencyKey) {

    public CreateBookingCommand {
        if (passengerId == null) {
            throw new IllegalArgumentException("A passenger must be named");
        }
        if (flightId == null) {
            throw new IllegalArgumentException("A flight must be named");
        }
        if (seats == null) {
            throw new IllegalArgumentException("A number of seats must be given");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("An idempotency key must be given");
        }
    }
}
