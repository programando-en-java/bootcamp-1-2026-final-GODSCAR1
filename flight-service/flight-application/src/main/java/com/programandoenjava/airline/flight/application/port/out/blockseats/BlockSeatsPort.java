package com.programandoenjava.airline.flight.application.port.out;

import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsCommand;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

import java.time.Instant;

public interface BlockSeatsPort {

    SeatBlock block(BlockSeatsCommand command, Instant now);
}
