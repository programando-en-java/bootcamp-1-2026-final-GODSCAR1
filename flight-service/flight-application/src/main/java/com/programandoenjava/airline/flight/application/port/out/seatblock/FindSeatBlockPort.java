package com.programandoenjava.airline.flight.application.port.out.seatblock;

import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlockId;

import java.util.Optional;

public interface FindSeatBlockPort {

    Optional<SeatBlock> byIdempotencyKey(IdempotencyKey key);

    boolean existsForBooking(BookingId bookingId);

    /** The flight is part of the question: a block on another flight is not found. */
    Optional<SeatBlock> byIdOnFlight(SeatBlockId id, FlightId flightId);
}
