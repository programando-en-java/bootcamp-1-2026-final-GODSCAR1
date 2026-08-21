package com.programandoenjava.airline.flight.application.port.in.blockseats;

public interface BlockSeatsUseCase {

    SeatsHeld block(BlockSeatsCommand command);
}
