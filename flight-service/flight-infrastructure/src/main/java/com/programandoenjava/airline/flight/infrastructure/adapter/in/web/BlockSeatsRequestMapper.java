package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsCommand;
import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatCount;
import com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto.BlockSeatsRequest;

import java.util.UUID;

final class BlockSeatsRequestMapper {

    private BlockSeatsRequestMapper() {
    }

    static BlockSeatsCommand toCommand(final UUID flightId,
                                       final String idempotencyKey,
                                       final BlockSeatsRequest request) {
        FlightId flight = new FlightId(flightId);
        BookingId booking = new BookingId(request.bookingId());
        SeatCount seats = new SeatCount(request.seats());
        IdempotencyKey key = new IdempotencyKey(idempotencyKey);

        return new BlockSeatsCommand(flight, booking, seats, key);
    }
}
