package com.programandoenjava.airline.booking.application.port.out.releaseseats;

import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;

/**
 * Gives held seats back to flight-service. Releasing a hold that is not there
 * succeeds, so this can be called again after a failure without checking first.
 */
public interface ReleaseSeatsPort {

    void release(FlightId flightId, SeatBlockId seatBlockId);
}
