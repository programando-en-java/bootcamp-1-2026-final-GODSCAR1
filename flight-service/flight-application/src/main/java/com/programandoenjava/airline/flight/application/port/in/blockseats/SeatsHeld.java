package com.programandoenjava.airline.flight.application.port.in.blockseats;

import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;
import com.programandoenjava.airline.flight.domain.shared.Money;

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
