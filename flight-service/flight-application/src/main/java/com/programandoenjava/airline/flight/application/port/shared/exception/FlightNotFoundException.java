package com.programandoenjava.airline.flight.application.port.shared.exception;

import com.programandoenjava.airline.flight.domain.flight.FlightId;

public class FlightNotFoundException extends RuntimeException {

    public FlightNotFoundException(final FlightId flightId) {
        super("No flight with id " + flightId.value());
    }
}
