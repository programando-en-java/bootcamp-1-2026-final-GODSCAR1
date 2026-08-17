package com.programandoenjava.airline.flight.application.port.in.blockseats;

import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

public interface BlockSeatsUseCase {

    SeatBlock block(BlockSeatsCommand command);
}
