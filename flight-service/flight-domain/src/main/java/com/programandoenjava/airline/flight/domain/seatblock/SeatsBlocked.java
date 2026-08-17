package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

public record BlockResult(Flight flight, SeatBlock block) {
}
