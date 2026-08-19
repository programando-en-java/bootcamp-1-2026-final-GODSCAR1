package com.programandoenjava.airline.flight.application.port.in.releaseseats;

import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlockId;

public record ReleaseSeatsCommand(FlightId flightId, SeatBlockId seatBlockId) {

    public ReleaseSeatsCommand {
        if (flightId == null) {
            throw new IllegalArgumentException("A flight must be named");
        }
        if (seatBlockId == null) {
            throw new IllegalArgumentException("A seat block must be named");
        }
    }
}
