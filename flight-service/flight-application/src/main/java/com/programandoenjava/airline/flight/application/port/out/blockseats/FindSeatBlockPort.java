package com.programandoenjava.airline.flight.application.port.out;

import com.programandoenjava.airline.flight.application.port.in.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

import java.util.Optional;

public interface FindSeatBlockPort {

    Optional<SeatBlock> byIdempotencyKey(IdempotencyKey key);
}
