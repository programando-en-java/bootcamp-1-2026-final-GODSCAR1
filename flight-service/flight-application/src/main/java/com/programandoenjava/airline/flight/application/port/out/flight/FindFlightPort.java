package com.programandoenjava.airline.flight.application.port.out.flight;

import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.flight.FlightId;

import java.util.Optional;

public interface FindFlightPort {

    Optional<Flight> byId(FlightId id);
}
