package com.programandoenjava.airline.checkin.application.port.out.readflight;

import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;

public interface ReadFlightPort {

    FlightSnapshot byId(FlightId flightId);
}
