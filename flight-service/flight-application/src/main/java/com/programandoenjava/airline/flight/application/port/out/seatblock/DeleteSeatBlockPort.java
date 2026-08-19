package com.programandoenjava.airline.flight.application.port.out.seatblock;

import com.programandoenjava.airline.flight.domain.seatblock.SeatBlockId;

public interface DeleteSeatBlockPort {

    void delete(SeatBlockId id);
}
