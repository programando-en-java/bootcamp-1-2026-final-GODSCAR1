package com.programandoenjava.airline.flight.application.port.out.blockseats;

import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

public interface SaveSeatBlockPort {

    void save(SeatBlock block, IdempotencyKey key);
}