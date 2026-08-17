package com.programandoenjava.airline.flight.application.port.in.blockseats;

import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;
import com.programandoenjava.airline.flight.domain.shared.Money;

/**
 * Seats held, and what each one costs.
 *
 * <p>The price belongs to the flight, not to the block, so it is not on
 * SeatBlock. It comes back here because it has to be the fare at the instant the
 * seats were taken: a caller that looked it up afterwards could read a different
 * number than the one it reserved against, and would charge for something other
 * than what it sold.
 *
 * <p>Same reasoning as SeatsBlocked one layer down — an operation hands back
 * everything its decision produced, rather than leaving the caller to reassemble
 * it from a second question.
 */
public record SeatsHeld(SeatBlock block, Money pricePerSeat) {

    public SeatsHeld {
        if (block == null) {
            throw new IllegalArgumentException("A held block is required");
        }
        if (pricePerSeat == null) {
            throw new IllegalArgumentException("A price is required");
        }
    }
}
