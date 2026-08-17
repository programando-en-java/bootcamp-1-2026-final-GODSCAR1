package com.programandoenjava.airline.booking.application.port.out.holdseats;

import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.BookingId;
import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.SeatCount;

public record HoldSeatsCommand(FlightId flightId,
                               BookingId bookingId,
                               SeatCount seats,
                               IdempotencyKey idempotencyKey) {

    public HoldSeatsCommand {
        if (flightId == null) {
            throw new IllegalArgumentException("A flight must be named");
        }
        if (bookingId == null) {
            throw new IllegalArgumentException("A booking must be named");
        }
        if (seats == null) {
            throw new IllegalArgumentException("A number of seats must be given");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("An idempotency key must be given");
        }
    }
}
