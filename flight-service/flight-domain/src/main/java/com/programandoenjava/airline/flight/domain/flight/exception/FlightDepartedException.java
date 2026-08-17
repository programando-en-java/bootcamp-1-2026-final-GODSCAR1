package com.programandoenjava.airline.flight.domain.shared;

public class FlightDepartedException extends DomainValidationException {

    public FlightDepartedException(String message) {
        super(message);
    }
}
