package com.programandoenjava.airline.flight.application.blockseats;

import com.programandoenjava.airline.flight.application.port.in.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatCount;

public record BlockSeatsCommand(
        FlightId flightId,
        BookingId bookingId,
        SeatCount seats,
        IdempotencyKey idempotencyKey) {

    public BlockSeatsCommand {
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
