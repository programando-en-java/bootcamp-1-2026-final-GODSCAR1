package com.programandoenjava.airline.flight.application.port.in.readflight;

import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.flight.FlightId;

public interface ReadFlightUseCase {

    Flight byId(FlightId id);
}
