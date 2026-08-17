package com.programandoenjava.airline.flight.application.port.out.blockseats;

import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

import java.util.Optional;

public interface FindSeatBlockPort {

    Optional<SeatBlock> byIdempotencyKey(IdempotencyKey key);

    boolean existsForBooking(BookingId bookingId);
}
