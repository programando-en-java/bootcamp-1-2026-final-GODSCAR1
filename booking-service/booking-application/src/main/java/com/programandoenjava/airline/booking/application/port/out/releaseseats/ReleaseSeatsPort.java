package com.programandoenjava.airline.booking.application.port.out.releaseseats;

import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;

public interface ReleaseSeatsPort {

    void release(FlightId flightId, SeatBlockId seatBlockId);
}
