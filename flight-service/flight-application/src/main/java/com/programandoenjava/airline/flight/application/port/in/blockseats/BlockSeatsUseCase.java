package com.programandoenjava.airline.flight.application.port.in.blockseats;

/**
 * Holds seats on a flight so that a booking can be made against them.
 *
 * <p>Repeating a command that already succeeded returns what it produced rather
 * than taking further seats.
 */
public interface BlockSeatsUseCase {

    SeatsHeld block(BlockSeatsCommand command);
}
