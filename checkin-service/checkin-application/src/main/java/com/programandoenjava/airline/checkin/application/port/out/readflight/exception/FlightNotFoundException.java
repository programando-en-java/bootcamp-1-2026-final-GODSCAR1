package com.programandoenjava.airline.checkin.application.port.out.readflight.exception;

import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;

public class FlightNotFoundException extends RuntimeException {

    public FlightNotFoundException(final FlightId flightId) {
        super("No flight with id " + flightId.value());
    }
}
