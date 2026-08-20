package com.programandoenjava.airline.checkin.application.port.out.readflight;

import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;

/**
 * Returns the snapshot the pass is printed from, so the copy is taken at the
 * edge of the service and nothing inside works with a live flight.
 */
public interface ReadFlightPort {

    FlightSnapshot byId(FlightId flightId);
}
