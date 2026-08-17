package com.programandoenjava.airline.flight.domain.seatblock;

import com.programandoenjava.airline.flight.domain.flight.Flight;

public record SeatsBlocked(Flight flight, SeatBlock block) {
}
