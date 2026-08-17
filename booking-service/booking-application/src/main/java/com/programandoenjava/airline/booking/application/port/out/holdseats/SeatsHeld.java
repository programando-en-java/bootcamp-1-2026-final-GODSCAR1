package com.programandoenjava.airline.booking.application.port.out.holdseats;

import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import com.programandoenjava.airline.booking.domain.shared.Money;

public record SeatsHeld(SeatBlockId seatBlockId, Money pricePerSeat) {

    public SeatsHeld {
        if (seatBlockId == null) {
            throw new IllegalArgumentException("A seat block id is required");
        }
        if (pricePerSeat == null) {
            throw new IllegalArgumentException("A fare is required");
        }
    }
}
